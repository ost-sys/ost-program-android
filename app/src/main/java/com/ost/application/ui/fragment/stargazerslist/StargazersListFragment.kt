package com.ost.application.ui.fragment.stargazerslist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper.END
import androidx.recyclerview.widget.ItemTouchHelper.START
import androidx.recyclerview.widget.LinearLayoutManager
import com.ost.application.MainActivity
import com.ost.application.ProfileActivity
import com.ost.application.ProfileActivity.Companion.KEY_STARGAZER
import com.ost.application.ProfileActivity.Companion.KEY_TRANSITION_AVATAR
import com.ost.application.ProfileActivity.Companion.KEY_TRANSITION_CONTAINER
import com.ost.application.ProfileActivity.Companion.KEY_TRANSITION_NAME
import com.ost.application.R
import com.ost.application.data.StargazersRepo
import com.ost.application.databinding.FragmentStargazersListBinding
import com.ost.application.ui.core.base.BaseFragment
import com.ost.application.ui.core.toast
import com.ost.application.ui.core.util.isOnline
import com.ost.application.ui.core.util.launchAndRepeatWithViewLifecycle
import com.ost.application.ui.core.util.openUrl
import com.ost.application.ui.core.util.seslSetFastScrollerEnabledForApi24
import com.ost.application.ui.fragment.stargazerslist.StargazersListViewModel.Companion.SWITCH_TO_HPB_DELAY
import com.ost.application.ui.fragment.stargazerslist.StargazersListViewModel.LoadState
import com.ost.application.ui.fragment.stargazerslist.adapter.StargazersAdapter
import com.ost.application.ui.fragment.stargazerslist.adapter.StargazersListItemDecoration
import com.ost.application.ui.fragment.stargazerslist.model.StargazersListItemUiModel
import com.ost.application.ui.fragment.stargazerslist.util.updateIndexer
import dev.oneuiproject.oneui.delegates.AppBarAwareYTranslator
import dev.oneuiproject.oneui.delegates.ViewYTranslator
import dev.oneuiproject.oneui.ktx.configureItemSwipeAnimator
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.ktx.enableCoreSeslFeatures
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior
import dev.oneuiproject.oneui.layout.startActionMode
import dev.oneuiproject.oneui.layout.startSearchMode
import dev.oneuiproject.oneui.widget.TipPopup
import dev.oneuiproject.oneui.widget.TipPopup.Direction
import dev.oneuiproject.oneui.widget.TipPopup.Mode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class StargazersListFragment : BaseFragment(), ViewYTranslator by AppBarAwareYTranslator() {

    private var tipPopupShown = false
    private var tipPopup: TipPopup? = null
    private lateinit var stargazersAdapter: StargazersAdapter

    private var _binding: FragmentStargazersListBinding? = null
    private val binding  get() = _binding!!

    private lateinit var stargazersViewModel: StargazersListViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentStargazersListBinding.inflate(layoutInflater, container, false).also{
        _binding = it
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.STARTED)
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()
        configureSwipeRefresh()
        configureItemSwipeAnimator()
        setupFabClickListener()
        observeUIState()

        showFragmentMenu(!isHidden)
    }


    private fun configureRecyclerView() {
        binding.stargazersList.apply rv@{
            setLayoutManager(LinearLayoutManager(requireContext()))
            setAdapter(StargazersAdapter(requireContext()).also {
                it.setupOnClickListeners()
                stargazersAdapter = it
            })
            addItemDecoration(StargazersListItemDecoration(requireContext()))
            setItemAnimator(null)
            enableCoreSeslFeatures(fastScrollerEnabled = false)

            stargazersAdapter.configure(
                this,
                StargazersAdapter.Payload.SELECTION_MODE,
                onAllSelectorStateChanged = { stargazersViewModel.allSelectorStateFlow.value = it }
            )

            binding.fab.hideOnScroll(this@rv/*, binding.indexscrollView*/)

            binding.indexscrollView.apply {
                setIndexScrollMargin(0, 78.dpToPx(resources))
                attachToRecyclerView(this@rv)
            }
        }

        with((requireActivity() as MainActivity).drawerLayout.appBarLayout) {
            translateYWithAppBar(setOf(binding.nsvNoItem, binding.loadingPb), this, this@StargazersListFragment)
        }
    }

    private fun setupFabClickListener() {
        binding.fab.apply {
            setOnClickListener {
                if (!isOnline(requireContext())) {
                    tipPopup = TipPopup(binding.fab, Mode.NORMAL).apply {
                        setMessage(context.getString(R.string.no_internet_connection_detected))
                        setExpanded(true)
                        show(Direction.DEFAULT)
                    }
                } else {
                    tipPopup = TipPopup(binding.fab, Mode.NORMAL).apply {
                        setMessage(context.getString(R.string.star_repositories))
                        setAction("OK") { requireContext().openUrl("https://github.com/ost-sys")}
                        setExpanded(true)
                        show(Direction.TOP_LEFT)
                    }
                }
            }
        }
    }


    private fun configureSwipeRefresh() {
        binding.swiperefreshView.apply {
            seslSetRefreshOnce(true)
            setOnRefreshListener {
                stargazersViewModel.refreshStargazers()
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(SWITCH_TO_HPB_DELAY)
                    isRefreshing = false
                }
            }
        }

        binding.retryBtn.apply {
            setOnClickListener {
                if (!isOnline(requireContext())) {
                    tipPopup = TipPopup(binding.retryBtn, Mode.NORMAL).apply {
                        setMessage(context.getString(R.string.no_internet_connection_detected))
                        setExpanded(true)
                        show(Direction.DEFAULT)
                    }
                }
                stargazersViewModel.refreshStargazers(true)
                toast(context.getString(R.string.callback_ping))
            }
        }
    }


    private fun observeUIState(){
        val stargazersRepo = StargazersRepo(requireContext())
        val viewModelFactory = StargazersListViewModelFactory(stargazersRepo, requireContext().applicationContext)
        stargazersViewModel = ViewModelProvider(this, viewModelFactory)[StargazersListViewModel::class.java]

        launchAndRepeatWithViewLifecycle {
            launch {
                stargazersViewModel.stargazersListScreenStateFlow
                    .collectLatest {
                        when (it.loadState) {
                            LoadState.LOADING -> {
                                binding.loadingPb.isVisible = true
                                binding.horizontalPb.isVisible = false
                                binding.retryBtn.isVisible = false
                            }

                            LoadState.REFRESHING -> {
                                binding.loadingPb.isVisible = false
                                binding.horizontalPb.isVisible = true
                                binding.retryBtn.isVisible = false

                            }

                            LoadState.LOADED,
                            LoadState.ERROR -> {
                                binding.loadingPb.isVisible = false
                                binding.horizontalPb.isVisible = false
                                binding.retryBtn.isVisible =
                                    it.loadState == LoadState.ERROR && it.itemsList.isEmpty()
                            }
                        }
                        val itemsList = it.itemsList
                        stargazersAdapter.submitList(itemsList)
                        if (itemsList.isNotEmpty()) {
                            updateRecyclerViewVisibility(true, it.noItemText)
                            binding.indexscrollView.updateIndexer(itemsList)
                        } else {
                            updateRecyclerViewVisibility(false, it.noItemText)
                        }
                        stargazersAdapter.highlightWord = it.query
                    }
            }

            launch {
                stargazersViewModel.stargazerSettingsStateFlow
                    .collectLatest {
                        if (it.enableIndexScroll) {
                            binding.indexscrollView.isVisible = true
                            binding.stargazersList.seslSetFastScrollerEnabledForApi24(false)
                        } else {
                            binding.indexscrollView.isVisible = false
                            binding.stargazersList.seslSetFastScrollerEnabledForApi24(true)
                        }
                        binding.indexscrollView.apply {
                            setAutoHide(it.autoHideIndexScroll)
                            setIndexBarTextMode(it.isTextModeIndexScroll)
                        }
                        stargazersAdapter.searchHighlightColor = it.searchHighlightColor
                    }
            }
        }
    }


    private fun updateRecyclerViewVisibility(visible: Boolean, noItemText: String){
        binding.nsvNoItem.isVisible = !visible
        binding.stargazersList.isVisible = visible
        binding.indexscrollView.isVisible = visible && stargazersViewModel.isIndexScrollEnabled()
        binding.tvNoItem.text = noItemText
    }


    private fun StargazersAdapter.setupOnClickListeners(){
        onClickItem = { stargazer, position, vh ->
            if (isActionMode) {
                onToggleItem(stargazer.toStableId(), position)
            } else {
                when(stargazer){
                    is StargazersListItemUiModel.StargazerItem -> {
                        openProfileActivity(vh, stargazer)
                    }
                    else -> Unit
                }
            }
        }

        onLongClickItem = {
            if (!isActionMode) launchActionMode()
            binding.stargazersList.seslStartLongPressMultiSelection()
        }
    }

    private fun configureItemSwipeAnimator() {
        binding.stargazersList.configureItemSwipeAnimator(
            leftToRightLabel = getString(R.string.github),
            rightToLeftLabel = getString(R.string.website),
            leftToRightColor = resources.getColor(R.color.primary),
            rightToLeftColor = resources.getColor(R.color.tertiary),
            leftToRightDrawableRes = R.drawable.about_page_github,
            rightToLeftDrawableRes = R.drawable.ic_oui_internet_website,
            isLeftSwipeEnabled = {viewHolder ->
                viewHolder.itemViewType == StargazersListItemUiModel.StargazerItem.VIEW_TYPE
                        && !stargazersAdapter.isActionMode
            },
            isRightSwipeEnabled = {viewHolder ->
                viewHolder.itemViewType == StargazersListItemUiModel.StargazerItem.VIEW_TYPE
                        && !stargazersAdapter.isActionMode
            },
            onSwiped = { position, swipeDirection, _ ->
                val stargazer = (stargazersAdapter.getItemByPosition(position) as StargazersListItemUiModel.StargazerItem).stargazer
                if (swipeDirection == START) {
                    if (stargazer.blog.toString() == "" || !stargazer.blog.toString().startsWith("http")) {
                        toast(getString(R.string.url_is_not_detected))
                    } else {
                        requireContext().openUrl(stargazer.blog.toString())
                    }
                }
                if (swipeDirection == END) {
                    requireContext().openUrl(stargazer.html_url)
                }
                true
            }
        )
    }


    private fun launchActionMode(initialSelected: Array<Long>? = null) {
        with (requireActivity() as MainActivity) {
            binding.fab.isVisible = false
            drawerLayout.startActionMode(
                onInflateMenu = { menu ->
                    stargazersAdapter.onToggleActionMode(true, initialSelected)
                    requireActivity().menuInflater.inflate(R.menu.menu_stargazers_am, menu)
                },
                onEnd = {
                    stargazersAdapter.onToggleActionMode(false)
                    binding.fab.isVisible = !drawerLayout.isSearchMode
                },
                onSelectMenuItem = {
                    when (it.itemId) {
                        R.id.menu_contacts_am_share -> {
                            val selectedItems = stargazersAdapter.getSelectedItems()
                            val shareUrls = selectedItems
                                .filterIsInstance<StargazersListItemUiModel.StargazerItem>()
                                .joinToString(separator = "\n") { it.stargazer.getDisplayName() + " - " + it.stargazer.html_url }

                            if (shareUrls.isNotBlank()) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareUrls)
                                }
                                startActivity(Intent.createChooser(shareIntent,
                                    getString(R.string.share_stargazers_urls)))
                            }

                            (requireActivity() as MainActivity).drawerLayout.endActionMode()
                            true
                        }
                        else -> false
                    }
                },
                onSelectAll = { isChecked: Boolean -> stargazersAdapter.onToggleSelectAll(isChecked) },
                allSelectorStateFlow = stargazersViewModel.allSelectorStateFlow,
                keepSearchMode = stargazersViewModel.getKeepSearchModeOnActionMode()
            )
        }
    }

    private val menuProvider = object : MenuProvider {

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_main, menu)
            val searchMenuItem = menu.findItem(R.id.menu_apppicker_search)
            searchMenuItem.setVisible(true)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_apppicker_search -> {
                    (requireActivity() as MainActivity).drawerLayout
                        .launchSearchMode(stargazersViewModel.getSearchModeOnBackBehavior())
                    true
                }

                else -> return false
            }
        }
    }

    private fun ToolbarLayout.launchSearchMode(onBackBehavior: SearchModeOnBackBehavior) {
        startSearchMode(
            onBackBehavior = onBackBehavior,
            onQuery = { query, _ ->
                stargazersViewModel.setQuery(query)
                true
            },
            onStart = {
                searchView.queryHint = context.getString(R.string.search_user)
                binding.fab.isVisible = false
            },
            onEnd = {
                stargazersViewModel.setQuery("")
                binding.fab.isVisible = !isActionMode
            }
        )
    }

    private fun openProfileActivity(
        vh: StargazersAdapter. ViewHolder,
        stargazer: StargazersListItemUiModel.StargazerItem
    ) {
        suspendItemRipple(vh.itemView)
        val transitionName = stargazer.stargazer.id.toString()
        val transitionName2 = "${transitionName}1"
        val transitionName3 =  "${transitionName}2"
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            requireActivity(),
            Pair(vh.itemView, transitionName),
            Pair(vh.avatarView, transitionName2),
            Pair(vh.nameView, transitionName3)
        )
        requireActivity().startActivity(
            Intent(
                requireActivity(),
                ProfileActivity::class.java
            ).apply {
                putExtra(KEY_STARGAZER, stargazer.stargazer)
                putExtra(KEY_TRANSITION_CONTAINER, transitionName)
                putExtra(KEY_TRANSITION_AVATAR, transitionName2)
                putExtra(KEY_TRANSITION_NAME, transitionName3)
            }, options.toBundle()
        )
    }

    private fun suspendItemRipple(itemView: View){
        val backgroundBu = itemView.background
        itemView.postOnAnimation{itemView.background = null}
        itemView.postDelayed({ itemView.background = backgroundBu}, 1_000)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            showFragmentMenu(true)
        } else {
            showFragmentMenu(false)
        }
    }

    private fun showFragmentMenu(show: Boolean){
        (requireActivity() as MainActivity).apply {
            removeMenuProvider(menuProvider)
            if (show){
                addMenuProvider(
                    menuProvider,
                    viewLifecycleOwner,
                    Lifecycle.State.STARTED
                )
            }
        }
    }

    override fun getLayoutResId(): Int = R.layout.fragment_stargazers_list

    override fun getIconResId(): Int = dev.oneuiproject.oneui.R.drawable.ic_oui_star_outline

    override fun getTitle(): CharSequence = getString(R.string.stargazers)
}
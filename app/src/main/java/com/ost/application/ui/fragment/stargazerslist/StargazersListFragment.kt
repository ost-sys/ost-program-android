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
import com.google.android.material.snackbar.Snackbar
import com.ost.application.MainActivity
import com.ost.application.MainActivity.Companion.KEY_REPO_NAME
import com.ost.application.ProfileActivity
import com.ost.application.ProfileActivity.Companion.KEY_STARGAZER
import com.ost.application.ProfileActivity.Companion.KEY_TRANSITION_AVATAR
import com.ost.application.ProfileActivity.Companion.KEY_TRANSITION_CONTAINER
import com.ost.application.ProfileActivity.Companion.KEY_TRANSITION_NAME
import com.ost.application.R
import com.ost.application.OSTApp
import com.ost.application.data.StargazersRepo
import com.ost.application.data.model.FetchState
import com.ost.application.databinding.FragmentStargazersListBinding
import com.ost.application.ui.core.base.BaseFragment
import com.ost.application.ui.core.toast
import com.ost.application.ui.core.util.SharingUtils.share
import com.ost.application.ui.core.util.isOnline
import com.ost.application.ui.core.util.launchAndRepeatWithViewLifecycle
import com.ost.application.ui.core.util.openUrl
import com.ost.application.ui.core.util.seslSetFastScrollerEnabledForApi24
import com.ost.application.ui.fragment.stargazerslist.adapter.StargazersAdapter
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
import dev.oneuiproject.oneui.utils.ItemDecorRule
import dev.oneuiproject.oneui.utils.SemItemDecoration
import dev.oneuiproject.oneui.widget.TipPopup
import dev.oneuiproject.oneui.widget.TipPopup.Direction
import dev.oneuiproject.oneui.widget.TipPopup.Mode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class StargazersListFragment : BaseFragment(), ViewYTranslator by AppBarAwareYTranslator() {

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

        if (savedInstanceState == null) {
            arguments?.getString(KEY_REPO_NAME)?.let {
                stargazersViewModel.setRepoFilter(it)
            }
        }else{
            if (savedInstanceState.getBoolean(KEY_IS_ACTION_MODE)) {
                val selectedIds = savedInstanceState.getLongArray(KEY_ACTION_MODE_SELECTED_IDS)!!
                launchActionMode(selectedIds.toTypedArray())
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if ((requireActivity() as MainActivity).drawerLayout.isActionMode) {
            outState.putBoolean(KEY_IS_ACTION_MODE, true)
            outState.putLongArray(KEY_ACTION_MODE_SELECTED_IDS,
                stargazersAdapter.getSelectedIds().asSet().toLongArray())
        }
        super.onSaveInstanceState(outState)
    }


    private fun configureRecyclerView() {
        binding.stargazersList.apply rv@{
            setLayoutManager(LinearLayoutManager(requireContext()))
            setAdapter(StargazersAdapter(requireContext()).also {
                it.setupOnClickListeners()
                stargazersAdapter = it
            })
            addItemDecoration(
                SemItemDecoration(requireContext(),
                    dividerRule = ItemDecorRule.SELECTED{
                        it.itemViewType == StargazersListItemUiModel.StargazerItem.VIEW_TYPE
                    },
                    subHeaderRule = ItemDecorRule.SELECTED{
                        it.itemViewType == StargazersListItemUiModel.SeparatorItem.VIEW_TYPE
                    }
                ).apply { setDividerInsetStart(78.dpToPx(resources)) }
            )
            setItemAnimator(null)
            enableCoreSeslFeatures(fastScrollerEnabled = false)

            stargazersAdapter.configure(
                this,
                StargazersAdapter.Payload.SELECTION_MODE,
                onAllSelectorStateChanged = { stargazersViewModel.allSelectorStateFlow.value = it }
            )

            binding.fab.hideOnScroll(this@rv/*, binding.indexscrollView*/)

            binding.indexscrollView.attachToRecyclerView(this@rv)
        }

        translateYWithAppBar(
            setOf(binding.nsvNoItem, binding.loadingPb),
            (requireActivity() as MainActivity).drawerLayout.appBarLayout,
            this@StargazersListFragment
        )
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
            setOnRefreshListener {
                stargazersViewModel.refreshStargazers()
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(SWITCH_TO_HPB_DELAY)
                    isRefreshing = false
                    if (stargazersViewModel.stargazersListScreenStateFlow.value.fetchStatus == FetchState.REFRESHING) {
                        binding.horizontalPb.isVisible = true
                    }
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


    private fun observeUIState() {
        val stargazersRepo = StargazersRepo.getInstance(requireContext())
        val viewModelFactory = StargazersListViewModelFactory(
            stargazersRepo, requireContext().applicationContext as OSTApp
        )

        stargazersViewModel =
            ViewModelProvider(this, viewModelFactory)[StargazersListViewModel::class.java]

        launchAndRepeatWithViewLifecycle {
            launch {
                stargazersViewModel.stargazersListScreenStateFlow
                    .collectLatest {
                        val itemsList = it.itemsList

                        updateLoadingStateViews(it.fetchStatus, itemsList.isEmpty())
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
                        binding.stargazersList.seslSetFastScrollerEnabledForApi24(!it.enableIndexScroll)

                        binding.indexscrollView.apply {
                            setAutoHide(it.autoHideIndexScroll)
                            setIndexBarTextMode(it.isTextModeIndexScroll)
                            isVisible = it.enableIndexScroll
                        }

                        stargazersAdapter.searchHighlightColor = it.searchHighlightColor

                        val shouldAutoRefresh = it.lastRefresh != 0L &&
                                (System.currentTimeMillis() - it.lastRefresh) > 1000*60*60*15

                        if (shouldAutoRefresh){
                            //Just do it silently
                            stargazersViewModel.refreshStargazers(false)
                        }
                    }
            }

            launch {
                stargazersViewModel.userMessage
                    .collect{
                        if (it != null){
                            Snackbar.make(requireActivity().window.decorView, it, Snackbar.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    private var lastStateReceived: FetchState? = null

    private fun updateLoadingStateViews(loadState: FetchState, isEmpty: Boolean) {
        if (lastStateReceived == loadState) return
        lastStateReceived = loadState

        when (loadState) {
            FetchState.NOT_INIT -> Unit
            FetchState.INITING -> {
                binding.loadingPb.isVisible = true
            }
            FetchState.INIT_ERROR,
            FetchState.REFRESH_ERROR -> {
                binding.loadingPb.isVisible = false
                binding.horizontalPb.isVisible = false
                binding.retryBtn.isVisible = isEmpty
            }
            FetchState.INITED,
            FetchState.REFRESHED -> {
                binding.loadingPb.isVisible = false
                binding.horizontalPb.isVisible = false
                binding.retryBtn.isVisible = false
            }
            FetchState.REFRESHING -> {
                binding.retryBtn.isVisible = false

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
                        && (stargazersAdapter.getItemByPosition(viewHolder.layoutPosition)
                        as StargazersListItemUiModel.StargazerItem).stargazer.blog != ""
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
                onSelectMenuItem = { it ->
                    when (it.itemId) {
                        R.id.menu_contacts_am_share -> {
                            lifecycleScope.launch {
                                stargazersAdapter.getSelectedIds().asSet()
                                    .map { id -> id.toInt() }//convert back to stargazer's id
                                    .toIntArray()
                                    .let { stargazersViewModel.getStargazersById(it) }
                                    .map { it.asVCardFile(requireContext()) }
                                    .share(requireContext())
                                drawerLayout.endActionMode()
                            }
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
        private var menu: Menu? = null

        override fun onPrepareMenu(menu: Menu) {
            super.onPrepareMenu(menu)
            menu.findItem(R.id.menu_stargazers_search).setVisible(true)
        }
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_main, menu)
            this.menu = menu
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_stargazers_search -> {
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

    companion object {
        private const val TAG = "StargazersListFragment"
        const val SWITCH_TO_HPB_DELAY = 1_500L
        private const val KEY_IS_ACTION_MODE = "isActionMode"
        private const val KEY_ACTION_MODE_SELECTED_IDS = "selectedIds"
    }
}
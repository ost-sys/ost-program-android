package com.ost.application.ui.fragment;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.MatrixCursor;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.util.SeslRoundedCorner;
import androidx.appcompat.util.SeslSubheaderRoundedCorner;
import androidx.appcompat.view.menu.SeslMenuItem;
import androidx.core.view.MenuProvider;
import androidx.indexscroll.widget.SeslCursorIndexer;
import androidx.indexscroll.widget.SeslIndexScrollView;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ost.application.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import dev.oneuiproject.oneui.utils.IndexScrollUtils;
import dev.oneuiproject.oneui.widget.Separator;
import com.ost.application.ui.core.base.BaseFragment;

public class FriendsListFragment extends BaseFragment {

    private int mCurrentSectionIndex = 0;
    private RecyclerView mListView;
    private SeslIndexScrollView mIndexScrollView;

    private boolean mIsTextModeEnabled = false;
    private boolean mIsIndexBarPressed = false;
    private final Runnable mHideIndexBar = new Runnable() {
        @Override
        public void run() {
            IndexScrollUtils.animateVisibility(mIndexScrollView, false);
        }
    };

    private static class FriendData {
        String nickname;
        String url;

        FriendData(String nickname, String url) {
            this.nickname = nickname;
            this.url = url;
        }
    }

    private final FriendData[] friends = {
            new FriendData("B", null),
            new FriendData("Bohdan", "https://Bohdan157.github.io"),
            new FriendData("D", null),
            new FriendData("dsys1100", "https://dsys1100.github.io/"),
            new FriendData("DimaLQ", "https://www.tiktok.com/@dimalq_real?_t=8mrsrUJf1Aj&_r=1"),
            new FriendData("Danya Shagalin", "https://www.youtube.com/@DanyaShagalin"),
            new FriendData("Daniel Myslivets", "https://www.youtube.com/@DanielM"),
            new FriendData("H", null),
            new FriendData("Hackintosh_user", "https://hackintoshuser137.github.io"),
            new FriendData("HappyWin8", "https://www.youtube.com/channel/UCEUf6E02RlsxKlAxIbNzBfA"),
            new FriendData("K", null),
            new FriendData("kernel64", "https://xerix123456.github.io"),
            new FriendData("kirillgorev", "https://kirillgorev.fun"),
            new FriendData("L", null),
            new FriendData("localhosted (aka. Nerok)", "https://t.me/localhosted"),
            new FriendData("M", null),
            new FriendData("Melamit", "https://github.com/melamit"),
            new FriendData("P", null),
            new FriendData("pachadomenic", "https://pachdomenic.github.io"),
            new FriendData("pashtetusss", "https://gitHub.com/pashtetusss777"),
            new FriendData("R", null),
            new FriendData("Radomyr", "https://github.com/BRamil1"),
            new FriendData("Rivixal", "https://rivixal.github.io/"),
            new FriendData("T", null),
            new FriendData("The Ertor", "https://ertorworld.com/"),
            new FriendData("Tinelix", "https://tinelix.ru/"),
            new FriendData("Z", null),
            new FriendData("zhh4", "https://t.me/zh4eny"),
            new FriendData("А", null),
            new FriendData("Антон Шуруповёрт", "https://github.com/Anton-Aboba1234")
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mIndexScrollView = view.findViewById(R.id.indexscroll_view);
        initListView(view);
        initIndexScroll();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) {
            requireActivity().addMenuProvider(menuProvider, getViewLifecycleOwner(), Lifecycle.State.STARTED);
        } else {
            requireActivity().removeMenuProvider(menuProvider);
        }
    }

    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            MenuItem menuItem = menu.findItem(R.id.menu_indexscroll_text);
            menuItem.setVisible(true);
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.menu_indexscroll_text) {
                mIsTextModeEnabled = !mIsTextModeEnabled;
                if (mIsTextModeEnabled) {
                    menuItem.setTitle(R.string.hide_letters);
                } else {
                    menuItem.setTitle(R.string.show_letters);
                }
                ((SeslMenuItem) menuItem).setBadgeText(null);
                mIndexScrollView.setIndexBarTextMode(mIsTextModeEnabled);
                mIndexScrollView.invalidate();
                return true;
            }
            return false;
        }
    };


    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        final boolean isRtl = newConfig
                .getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        if (mIndexScrollView != null) {
            mIndexScrollView.setIndexBarGravity(isRtl
                    ? SeslIndexScrollView.GRAVITY_INDEX_BAR_LEFT
                    : SeslIndexScrollView.GRAVITY_INDEX_BAR_RIGHT);
        }
    }

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_friends_list;
    }

    @Override
    public int getIconResId() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_group_outline;
    }

    @Override
    public CharSequence getTitle() {
        return getString(R.string.friends);
    }

    private void initListView(@NonNull View view) {
        mListView = view.findViewById(R.id.indexscroll_list);
        mListView.setLayoutManager(new LinearLayoutManager(mContext));
        mListView.setAdapter(new IndexAdapter());
        mListView.addItemDecoration(new ItemDecoration(mContext));
        mListView.setItemAnimator(null);
        mListView.seslSetFillBottomEnabled(true);
        mListView.seslSetLastRoundedCorner(true);
        mListView.seslSetIndexTipEnabled(true);
        mListView.seslSetGoToTopEnabled(true);
        mListView.seslSetSmoothScrollEnabled(true);
    }

    private void initIndexScroll() {
        final boolean isRtl = getResources().getConfiguration()
                .getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;

        mIndexScrollView.setIndexBarGravity(isRtl
                ? SeslIndexScrollView.GRAVITY_INDEX_BAR_LEFT
                : SeslIndexScrollView.GRAVITY_INDEX_BAR_RIGHT);

        MatrixCursor cursor = new MatrixCursor(new String[]{"item"});
        for (FriendData friend : friends) {
            cursor.addRow(new String[]{friend.nickname});
        }

        cursor.moveToFirst();

        SeslCursorIndexer indexer = new SeslCursorIndexer(cursor, 0,
                "B,D,H,K,L,P,R,T,Z,А".split(","), 0);
        indexer.setMiscItemsCount(3);

        mIndexScrollView.setIndexer(indexer);
        mIndexScrollView.setOnIndexBarEventListener(
                new SeslIndexScrollView.OnIndexBarEventListener() {
                    @Override
                    public void onIndexChanged(int sectionIndex) {
                        if (mCurrentSectionIndex != sectionIndex) {
                            mCurrentSectionIndex = sectionIndex;
                            if (mListView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE) {
                                mListView.stopScroll();
                            }
                            ((LinearLayoutManager) Objects.requireNonNull(mListView.getLayoutManager()))
                                    .scrollToPositionWithOffset(sectionIndex, 0);
                        }
                    }

                    @Override
                    public void onPressed(float v) {
                        mIsIndexBarPressed = true;
                        mListView.removeCallbacks(mHideIndexBar);
                    }

                    @Override
                    public void onReleased(float v) {
                        mIsIndexBarPressed = false;
                        if (mListView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
                            mListView.postDelayed(mHideIndexBar, 1500);
                        }
                    }
                });
        mIndexScrollView.attachToRecyclerView(mListView);
        mListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE
                        && !mIsIndexBarPressed) {
                    recyclerView.postDelayed(mHideIndexBar, 1500);
                } else {
                    mListView.removeCallbacks(mHideIndexBar);
                    IndexScrollUtils.animateVisibility(mIndexScrollView, true);
                }
            }
        });
    }

    public class IndexAdapter extends RecyclerView.Adapter<IndexAdapter.ViewHolder>
            implements SectionIndexer {
        List<String> mSections = new ArrayList<>();
        List<Integer> mPositionForSection = new ArrayList<>();
        List<Integer> mSectionForPosition = new ArrayList<>();

        private long mLastClickTime;

        IndexAdapter() {
            mSections.add("");
            mPositionForSection.add(0);
            mSectionForPosition.add(0);

            for (int i = 1; i < friends.length; i++) {
                String letter = friends[i].nickname;
                if (letter.length() == 1) {
                    mSections.add(letter);
                    mPositionForSection.add(i);
                }
                mSectionForPosition.add(mSections.size() - 1);
            }
        }

        @Override
        public int getItemCount() {
            return friends.length;
        }

        @Override
        public int getItemViewType(int position) {
            return (friends[position].nickname.length() == 1) ? 1 : 0;
        }

        @NonNull
        @Override
        public IndexAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                View view = inflater.inflate(
                        R.layout.sample3_view_indexscroll_listview_item, parent, false);
                return new IndexAdapter.ViewHolder(view, false);
            } else {
                return new IndexAdapter.ViewHolder(new Separator(mContext), true);
            }
        }

        @Override
        public void onBindViewHolder(IndexAdapter.ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
            if (holder.isSeparator) {
                holder.textView.setLayoutParams(new LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            } else {
                int iconResId = getIconResource(friends[position].nickname);
                if (iconResId != 0) {
                    holder.imageView.setImageResource(iconResId);
                } else {
                    holder.imageView.setImageResource(R.drawable.ic_oui_info);
                }

                holder.itemView.setOnClickListener(v -> {
                    long uptimeMillis = SystemClock.uptimeMillis();
                    if (uptimeMillis - mLastClickTime > 600L) {
                        mLastClickTime = uptimeMillis;
                        String url = friends[position].url;
                        if (url != null) {
                            try {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(url));
                                startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                Log.d("Link error", "Activity is not detected");
                            }
                        }
                    }
                });
            }

            holder.textView.setText(friends[position].nickname);
        }
        private int getIconResource(String nickname) {
            int iconResId = 0;

            switch (nickname) {
                case "dsys1100":
                    iconResId = R.drawable.dsys1100;
                    break;
                case "localhosted (aka. Nerok)":
                    iconResId = R.drawable.localhosted;
                    break;
                case "DimaLQ":
                    iconResId = R.drawable.dimalq;
                    break;
                case "Bohdan":
                    iconResId = R.drawable.bohdan;
                    break;
                case "kernel64":
                    iconResId = R.drawable.kernel64;
                    break;
                case "Hackintosh_user":
                    iconResId = R.drawable.hackuser;
                    break;
                case "The Ertor":
                    iconResId = R.drawable.ertor;
                    break;
                case "HappyWin8":
                    iconResId = R.drawable.happywin8;
                    break;
                case "Rivixal":
                    iconResId = R.drawable.rivixal;
                    break;
                case "Антон Шуруповёрт":
                    iconResId = R.drawable.anton;
                    break;
                case "pachadomenic":
                    iconResId = R.drawable.tu4wkl;
                    break;
                case "zhh4":
                    iconResId = R.drawable.zhh4;
                    break;
                case "melamit":
                    iconResId = R.drawable.melamit;
                    break;
                case "Daniel Myslivets":
                    iconResId = R.drawable.danielm;
                    break;
                case "Tinelix":
                    iconResId = R.drawable.tinelix;
                    break;
                case "Danya Shagalin":
                    iconResId = R.drawable.shagalin;
                    break;
                case "Radomyr":
                    iconResId = R.drawable.radomyr;
                    break;
                case "pashtetusss":
                    iconResId = R.drawable.pashtetusss;
                    break;
                case "Melamit":
                    iconResId = R.drawable.melamit;
                    break;
                case "kirillgorev":
                    iconResId = R.drawable.kirillgorev;
                    break;
            }

            return iconResId;
        }

        @Override
        public Object[] getSections() {
            return mSections.toArray();
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            return mPositionForSection.get(sectionIndex);
        }

        @Override
        public int getSectionForPosition(int position) {
            return mSectionForPosition.get(position);
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            boolean isSeparator;
            ImageView imageView;
            TextView textView;

            ViewHolder(View itemView, boolean isSeparator) {
                super(itemView);
                this.isSeparator = isSeparator;
                if (isSeparator) {
                    textView = (TextView) itemView;
                } else {
                    imageView = itemView.findViewById(R.id.indexscroll_list_item_icon);
                    textView = itemView.findViewById(R.id.indexscroll_list_item_text);
                }
            }
        }
    }

    private class ItemDecoration extends RecyclerView.ItemDecoration {
        private final Drawable mDivider;
        private final SeslSubheaderRoundedCorner mRoundedCorner;

        @SuppressLint({"PrivateResource", "UseCompatLoadingForDrawables"})
        public ItemDecoration(@NonNull Context context) {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(androidx.appcompat.R.attr.isLightTheme, outValue, true);

            mDivider = context.getDrawable(outValue.data == 0
                    ? androidx.appcompat.R.drawable.sesl_list_divider_dark
                    : androidx.appcompat.R.drawable.sesl_list_divider_light);

            mRoundedCorner = new SeslSubheaderRoundedCorner(mContext);
            mRoundedCorner.setRoundedCorners(SeslRoundedCorner.ROUNDED_CORNER_ALL);
        }

        @Override
        public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent,
                           @NonNull RecyclerView.State state) {
            super.onDraw(c, parent, state);

            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                IndexAdapter.ViewHolder holder
                        = (IndexAdapter.ViewHolder) mListView.getChildViewHolder(child);
                if (!holder.isSeparator) {
                    final int top = child.getBottom()
                            + ((ViewGroup.MarginLayoutParams) child.getLayoutParams()).bottomMargin;
                    final int bottom = mDivider.getIntrinsicHeight() + top;

                    mDivider.setBounds(parent.getLeft(), top, parent.getRight(), bottom);
                    mDivider.draw(c);
                }
            }
        }

        @Override
        public void seslOnDispatchDraw(@NonNull Canvas c, RecyclerView parent, RecyclerView.State state) {
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                IndexAdapter.ViewHolder holder
                        = (IndexAdapter.ViewHolder) mListView.getChildViewHolder(child);
                if (holder.isSeparator) {
                    mRoundedCorner.drawRoundedCorner(child, c);
                }
            }
        }
    }
}
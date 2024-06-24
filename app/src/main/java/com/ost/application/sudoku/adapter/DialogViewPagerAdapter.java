package com.ost.application.sudoku.adapter;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.ost.application.R;
import com.ost.application.sudoku.dialog.NewSudokuDialog;
import com.ost.application.sudoku.dialog.Tab_Generate;
import com.ost.application.sudoku.dialog.Tab_Import;
import com.ost.application.sudoku.dialog.Tab_Make_Own;
import com.ost.application.sudoku.utils.DynamicViewPager;

public class DialogViewPagerAdapter extends FragmentPagerAdapter {

    private String[] tab_names;
    private Fragment[] fragments;

    public DialogViewPagerAdapter(@NonNull FragmentManager fm, NewSudokuDialog.DialogListener dialogListener, Context context) {
        super(fm);
        tab_names = new String[]{context.getString(R.string.generate), context.getString(R.string._import), context.getString(R.string.make_own)};
        fragments = new Fragment[]{new Tab_Generate(dialogListener), new Tab_Import(dialogListener), new Tab_Make_Own(dialogListener)};
    }

    @Override
    public Fragment getItem(int position) {
        return fragments[position];
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tab_names[position];
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);
        Fragment fragment = (Fragment) object;
        DynamicViewPager pager = (DynamicViewPager) container;
        if (fragment != null && fragment.getView() != null) {
            pager.measureCurrentView(fragment.getView());
        }
    }


}

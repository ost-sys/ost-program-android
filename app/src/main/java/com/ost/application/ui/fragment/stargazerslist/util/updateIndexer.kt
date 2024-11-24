package com.ost.application.ui.fragment.stargazerslist.util

import android.database.MatrixCursor
import androidx.indexscroll.widget.SeslCursorIndexer
import androidx.indexscroll.widget.SeslIndexScrollView
import com.ost.application.ui.fragment.stargazerslist.model.StargazersListItemUiModel

internal fun SeslIndexScrollView.updateIndexer(items: List<StargazersListItemUiModel>) {
        val cursor = MatrixCursor(arrayOf("item"))
        for (item in items.toStringsList()){
            cursor.addRow(arrayOf(item))
        }
        val indexChars = items.toIndexCharsArray()

        cursor.moveToFirst()
        setIndexer(SeslCursorIndexer(cursor, 0,indexChars, 0).apply {
                //setGroupItemsCount(1)
                //setMiscItemsCount(3)
            }
        )
        postInvalidate()
    }
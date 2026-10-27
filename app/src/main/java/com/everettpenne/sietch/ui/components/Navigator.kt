package com.everettpenne.sietch.ui.components

import com.everettpenne.sietch.data.Category

/** Where a tap goes. Pure callbacks, so screens do not know about navigation. */
interface Navigator {
    /** Open a folder by its absolute path. */
    fun openFolder(path: String)
    /** Pop back to an ancestor folder already on the navigation stack. */
    fun backTo(path: String)
    fun openCategory(category: Category)
    fun openTrash()
    fun openSearch()
    fun openSettings()
}

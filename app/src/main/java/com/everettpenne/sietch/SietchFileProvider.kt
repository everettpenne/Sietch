package com.everettpenne.sietch

import androidx.core.content.FileProvider

/**
 * Sietch's own provider for handing one file to another app. It is a distinct class from the update library's FileProvider because two providers
 * of the same class would be merged into one by the manifest merger, and the update library's has its own paths and authority.
 */
class SietchFileProvider : FileProvider()

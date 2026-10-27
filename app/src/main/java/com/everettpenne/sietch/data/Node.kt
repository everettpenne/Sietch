package com.everettpenne.sietch.data

/** One file or folder, identified by its absolute path. Everything the lists show is one of these. */
data class Node(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    /** Last modified, in milliseconds since the epoch (0 when unknown). */
    val modified: Long,
    val mime: String? = null,
)

package com.example.model

data class IspPortal(
    val id: String,
    val name: String,
    val shortName: String,
    val url: String,
    val username: String,
    val password: String,
    val partnerCode: String = "",
    val notes: String = "",
    val category: String = "FMS",
    val primaryColorHex: Long = 0xFF1E40AF
)

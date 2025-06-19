package com.wayneenterprises.habitum.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class User(
    val id: String = "",
    @SerialName("auth_id") val authId: String = "",
    val name: String = "",
    val email: String = "",
    @SerialName("user_type") val userType: String = "normal", // "admin" o "normal"
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class UserInsert(
    @SerialName("auth_id") val authId: String,
    val name: String,
    val email: String,
    @SerialName("user_type") val userType: String = "normal"
)

@Serializable
data class UserUpdate(
    val name: String? = null,
    @SerialName("user_type") val userType: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
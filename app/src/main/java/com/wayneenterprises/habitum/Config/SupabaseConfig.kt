package com.wayneenterprises.habitum.Config

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseConfig {

    // TODO: Reemplaza estos valores con los de tu proyecto Supabase
    private const val SUPABASE_URL = "https://akloycbkzcttqkzbhccu.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFrbG95Y2JremN0dHFremJoY2N1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTAyMTY5NzksImV4cCI6MjA2NTc5Mjk3OX0.3mhZxQrcpP3A9nGrNW02x3sUatY6LxxbgvwjsK7nys0"

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
        install(Auth)
    }
}
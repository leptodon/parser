package app

import data.api.KickstarterApiClient

fun main() {
    println("Kickstarter Project Parser [Kotlin]")
//    print("Введите токен авторизации: ")
//    val token = readlnOrNull() ?: return
    val api = KickstarterApiClient("3a51ab050410e7566cb3ae51217f87eb48b59b46")
    ConsoleApp(api).run()
}

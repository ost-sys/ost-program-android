package com.ost.application.data.model // Убедись что пакет правильный

import android.util.Log // Добавь лог
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import retrofit2.HttpException // Для отлова HTTP ошибок

object NetworkDataSource {

    private const val TAG = "NetworkDataSource" // Тег для логов

    // Этот метод возвращает Result, обрабатывая ошибки из getStargazersInternal
    suspend fun fetchStargazers(): Result<List<Stargazer>> = runCatching {
        Log.d(TAG, "fetchStargazers called")
        getStargazersInternal() // Вызываем внутренний метод
    }.onFailure {
        Log.e(TAG, "fetchStargazers failed", it) // Логируем общую ошибку здесь
    }.onSuccess {
        Log.d(TAG, "fetchStargazers success, count: ${it.size}")
    }

    // Внутренний метод, который делает реальную работу и может кинуть исключение
    private suspend fun getStargazersInternal(): List<Stargazer> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting getStargazersInternal...")
        val repoList = listOf(
            "ost-sys.github.io",
            "ost-program-android"
        )
        Log.d(TAG, "Repo list: $repoList")

        // Используем RetrofitClient напрямую, так как он объект
        // Убедись, что RetrofitClient.instance возвращает GitHubService
        val service = RetrofitClient.instance

        // Оборачиваем всю логику в try-catch на случай непредвиденных ошибок
        try {
            val stargazerList = repoList.map { repoName ->
                async { // Запускаем получение звёзд для каждого репо параллельно
                    try {
                        Log.d(TAG, "Fetching stargazers for $repoName")
                        // Получаем список звёзд для одного репо
                        val result = service.getStargazers("ost-sys", repoName).map { stargazer ->
                            stargazer to repoName // Добавляем имя репо к каждой звезде
                        }
                        Log.d(TAG, "Fetched ${result.size} for $repoName")
                        result
                    } catch (e: HttpException) {
                        Log.e(TAG, "HTTP error fetching stargazers for $repoName: ${e.code()}", e)
                        emptyList<Pair<Stargazer, String>>() // Возвращаем пустой список для этого репо
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to fetch stargazers for $repoName", e)
                        emptyList<Pair<Stargazer, String>>() // Возвращаем пустой список
                    }
                }
            }.awaitAll().flatten() // Ждем завершения всех запросов и объединяем результаты

            Log.d(TAG, "Total stargazers fetched (flat): ${stargazerList.size}")

            if (stargazerList.isEmpty()) {
                Log.w(TAG, "No stargazers fetched from any repo.")
                return@withContext emptyList() // Если список пуст, выходим
            }

            // Группируем по логину, чтобы объединить репозитории для одного юзера
            val stargazerMap = stargazerList.groupBy(
                keySelector = { it.first.login }, // Ключ - логин
                valueTransform = { it } // Значение - пара (Stargazer, repoName)
            )

            // Объединяем информацию: один Stargazer со списком его starredRepos
            val mergedStargazers = stargazerMap.map { (_, stargazerTuples) ->
                val stargazer = stargazerTuples.first().first // Берем объект Stargazer (он одинаковый для одного логина)
                val starredRepos = stargazerTuples.map { it.second }.toSet() // Собираем уникальные имена репо
                stargazer.copy(starredRepos = starredRepos) // Обновляем поле starredRepos
            }
            Log.d(TAG, "Merged stargazers count: ${mergedStargazers.size}")

            // Получаем детали для каждого уникального звездочета
            val detailedStargazers = mergedStargazers.map { stargazer ->
                async { // Запускаем получение деталей параллельно
                    try {
                        Log.d(TAG, "Getting details for ${stargazer.login}")
                        val userDetails = service.getUserDetails(stargazer.login) // Получаем детали
                        Log.d(TAG, "Got details for ${stargazer.login}")
                        // Обновляем объект Stargazer деталями
                        stargazer.copy(
                            name = userDetails.name,
                            location = userDetails.location,
                            company = userDetails.company,
                            email = userDetails.email,
                            twitter_username = userDetails.twitter_username,
                            blog = userDetails.blog,
                            bio = userDetails.bio,
                        )
                    } catch (e: HttpException) {
                        // Логируем HTTP ошибку (например, 404 Not Found), но не прерываем процесс
                        Log.w(TAG, "HTTP error getting details for ${stargazer.login}: ${e.code()}", e)
                        stargazer // Возвращаем Stargazer без деталей
                    } catch (e: Exception) {
                        // Логируем другую ошибку, но не прерываем процесс
                        Log.e(TAG, "Failed to get details for ${stargazer.login}", e)
                        stargazer // Возвращаем Stargazer без деталей
                    }
                }
            }.awaitAll() // Ждем завершения всех запросов деталей

            Log.d(TAG, "Detailed stargazers count: ${detailedStargazers.size}")

            // Сортируем финальный список
            val sortedList = detailedStargazers.sortedWith(compareBy(
                // Сначала цифры и символы (isDigit вернет false для символов)
                { it.getDisplayName().firstOrNull()?.isLetter() == false },
                // Потом по имени без учета регистра
                { it.getDisplayName().uppercase() }
            ))
            Log.d(TAG, "Sorted list size: ${sortedList.size}")
            return@withContext sortedList // Возвращаем отсортированный список

        } catch (e: Exception) {
            // Ловим любые другие ошибки на верхнем уровне
            Log.e(TAG, "Error in getStargazersInternal main processing block", e)
            return@withContext emptyList<Stargazer>() // Возвращаем пустой список
        }
    }
}
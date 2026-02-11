package com.rasoiai.data.local.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rasoiai.data.local.entity.ChatMessageEntity
import com.rasoiai.data.local.entity.FavoriteCollectionEntity
import com.rasoiai.data.local.entity.FavoriteEntity
import com.rasoiai.data.local.entity.GroceryItemEntity
import com.rasoiai.data.local.entity.MealPlanEntity
import com.rasoiai.data.local.entity.PantryItemEntity
import com.rasoiai.data.local.entity.MealPlanFestivalEntity
import com.rasoiai.data.local.entity.CookingStreakEntity
import com.rasoiai.data.local.entity.CookingDayEntity
import com.rasoiai.data.local.entity.AchievementEntity
import com.rasoiai.data.local.entity.WeeklyChallengeEntity
import com.rasoiai.data.local.entity.RecipeRuleEntity
import com.rasoiai.data.local.entity.NutritionGoalEntity
import com.rasoiai.data.local.entity.MealPlanItemEntity
import com.rasoiai.data.local.entity.RecipeEntity
import com.rasoiai.data.remote.dto.FestivalDto
import com.rasoiai.data.remote.dto.IngredientDto
import com.rasoiai.data.remote.dto.InstructionDto
import com.rasoiai.data.remote.dto.MealItemDto
import com.rasoiai.data.remote.dto.MealPlanDayDto
import com.rasoiai.data.remote.dto.MealPlanResponse
import com.rasoiai.data.remote.dto.NutritionDto
import com.rasoiai.data.remote.dto.RecipeResponse
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.Difficulty
import com.rasoiai.domain.model.Festival
import com.rasoiai.domain.model.FavoriteCollection
import com.rasoiai.domain.model.GroceryItem
import com.rasoiai.domain.model.GroceryList
import com.rasoiai.domain.model.Ingredient
import com.rasoiai.domain.model.IngredientCategory
import com.rasoiai.domain.model.Instruction
import com.rasoiai.domain.model.MealItem
import com.rasoiai.domain.model.MealPlan
import com.rasoiai.domain.model.MealPlanDay
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.model.Nutrition
import com.rasoiai.domain.model.PantryCategory
import com.rasoiai.domain.model.PantryItem
import com.rasoiai.domain.model.Recipe
import com.rasoiai.domain.model.ChatMessage
import com.rasoiai.domain.model.RecipeSuggestion
import com.rasoiai.domain.model.CookingStreak
import com.rasoiai.domain.model.CookingDay
import com.rasoiai.domain.model.Achievement
import com.rasoiai.domain.model.WeeklyChallenge
import com.rasoiai.domain.model.RecipeRule
import com.rasoiai.domain.model.RuleType
import com.rasoiai.domain.model.RuleAction
import com.rasoiai.domain.model.RuleEnforcement
import com.rasoiai.domain.model.RuleFrequency
import com.rasoiai.domain.model.FrequencyType
import com.rasoiai.domain.model.NutritionGoal
import com.rasoiai.domain.model.FoodCategory
import com.rasoiai.domain.model.Notification
import com.rasoiai.domain.model.NotificationActionData
import com.rasoiai.domain.model.NotificationActionType
import com.rasoiai.domain.model.NotificationType
import com.rasoiai.domain.model.OfflineAction
import com.rasoiai.domain.model.ActionStatus
import com.rasoiai.domain.model.OfflineActionType
import com.rasoiai.data.local.entity.NotificationEntity
import com.rasoiai.data.local.entity.OfflineQueueEntity
import com.rasoiai.data.remote.dto.NotificationDto
import com.rasoiai.data.remote.dto.NotificationActionDataDto
import com.rasoiai.data.remote.dto.NutritionGoalDto
import com.rasoiai.data.remote.dto.RecipeRuleDto
import com.rasoiai.data.remote.dto.RecipeRuleSyncItem
import com.rasoiai.data.remote.dto.NutritionGoalSyncItem
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val gson = Gson()

// ==================== DTO to Entity Mappers ====================

fun MealPlanResponse.toEntity(): MealPlanEntity = MealPlanEntity(
    id = id,
    weekStartDate = weekStartDate,
    weekEndDate = weekEndDate,
    createdAt = parseTimestamp(createdAt),
    updatedAt = parseTimestamp(updatedAt),
    isSynced = true
)

fun MealPlanResponse.toItemEntities(): List<MealPlanItemEntity> {
    Timber.d("toItemEntities: Processing ${days.size} days from API response")
    return days.flatMap { day ->
        val items = mutableListOf<MealPlanItemEntity>()
        Timber.d("toItemEntities: Day ${day.date} - B=${day.meals.breakfast.size}, L=${day.meals.lunch.size}, D=${day.meals.dinner.size}, S=${day.meals.snacks.size}")

        day.meals.breakfast.forEachIndexed { index, item ->
            items.add(item.toEntity(id, day.date, day.dayName, MealType.BREAKFAST.value, index))
        }
        day.meals.lunch.forEachIndexed { index, item ->
            items.add(item.toEntity(id, day.date, day.dayName, MealType.LUNCH.value, index))
        }
        day.meals.dinner.forEachIndexed { index, item ->
            items.add(item.toEntity(id, day.date, day.dayName, MealType.DINNER.value, index))
        }
        day.meals.snacks.forEachIndexed { index, item ->
            items.add(item.toEntity(id, day.date, day.dayName, MealType.SNACKS.value, index))
        }

        items
    }
}

fun MealPlanResponse.toFestivalEntities(): List<MealPlanFestivalEntity> {
    return days.mapNotNull { day ->
        day.festival?.toEntity(id, day.date)
    }
}

fun MealItemDto.toEntity(
    mealPlanId: String,
    date: String,
    dayName: String,
    mealType: String,
    order: Int
): MealPlanItemEntity = MealPlanItemEntity(
    id = id,
    mealPlanId = mealPlanId,
    date = date,
    dayName = dayName,
    mealType = mealType,
    recipeId = recipeId,
    recipeName = recipeName,
    recipeImageUrl = recipeImageUrl,
    prepTimeMinutes = prepTimeMinutes,
    calories = calories,
    dietaryTags = dietaryTags,
    isLocked = isLocked,
    order = order
)

fun FestivalDto.toEntity(mealPlanId: String, date: String): MealPlanFestivalEntity = MealPlanFestivalEntity(
    id = id,
    mealPlanId = mealPlanId,
    date = date,
    name = name,
    isFastingDay = isFastingDay,
    suggestedDishes = suggestedDishes ?: emptyList()
)

// ==================== Entity to Domain Mappers ====================

fun MealPlanEntity.toDomain(
    items: List<MealPlanItemEntity>,
    festivals: List<MealPlanFestivalEntity>
): MealPlan {
    Timber.d("toDomain: Converting ${items.size} items, ${festivals.size} festivals")

    val dateFormatter = DateTimeFormatter.ISO_DATE
    val startDate = LocalDate.parse(weekStartDate, dateFormatter)
    val endDate = LocalDate.parse(weekEndDate, dateFormatter)

    // Group items by date
    val itemsByDate = items.groupBy { it.date }
    Timber.d("toDomain: itemsByDate keys = ${itemsByDate.keys}")
    val festivalsByDate = festivals.associateBy { it.date }

    // Create days for each date in range
    val days = generateSequence(startDate) { it.plusDays(1) }
        .takeWhile { !it.isAfter(endDate) }
        .map { date ->
            val dateStr = date.format(dateFormatter)
            val dayItems = itemsByDate[dateStr] ?: emptyList()
            Timber.d("toDomain: Date $dateStr has ${dayItems.size} items")
            val festival = festivalsByDate[dateStr]

            MealPlanDay(
                date = date,
                dayName = dayItems.firstOrNull()?.dayName ?: date.dayOfWeek.name.lowercase()
                    .replaceFirstChar { it.uppercase() },
                breakfast = dayItems.filter { it.mealType == MealType.BREAKFAST.value }
                    .sortedBy { it.order }.map { it.toDomain() },
                lunch = dayItems.filter { it.mealType == MealType.LUNCH.value }
                    .sortedBy { it.order }.map { it.toDomain() },
                dinner = dayItems.filter { it.mealType == MealType.DINNER.value }
                    .sortedBy { it.order }.map { it.toDomain() },
                snacks = dayItems.filter { it.mealType == MealType.SNACKS.value }
                    .sortedBy { it.order }.map { it.toDomain() },
                festival = festival?.toDomain(date)
            )
        }
        .toList()

    return MealPlan(
        id = id,
        weekStartDate = startDate,
        weekEndDate = endDate,
        days = days,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun MealPlanItemEntity.toDomain(): MealItem = MealItem(
    id = "$mealPlanId-$date-$mealType-$recipeId",
    recipeId = recipeId,
    recipeName = recipeName,
    recipeImageUrl = recipeImageUrl,
    prepTimeMinutes = prepTimeMinutes,
    calories = calories,
    isLocked = isLocked,
    order = order,
    dietaryTags = dietaryTags.mapNotNull { DietaryTag.fromValue(it) }
)

fun MealPlanFestivalEntity.toDomain(date: LocalDate): Festival = Festival(
    id = id,
    name = name,
    date = date,
    isFastingDay = isFastingDay,
    suggestedDishes = suggestedDishes
)

// ==================== Recipe DTO to Entity Mappers ====================

fun RecipeResponse.toEntity(isFavorite: Boolean = false): RecipeEntity = RecipeEntity(
    id = id,
    name = name,
    description = description,
    imageUrl = imageUrl,
    prepTimeMinutes = prepTimeMinutes,
    cookTimeMinutes = cookTimeMinutes,
    servings = servings,
    difficulty = difficulty,
    cuisineType = cuisineType,
    mealTypes = mealTypes,
    dietaryTags = dietaryTags,
    ingredients = gson.toJson(ingredients),
    instructions = gson.toJson(instructions),
    nutritionInfo = nutrition?.let { gson.toJson(it) },
    calories = nutrition?.calories,
    isFavorite = isFavorite,
    cachedAt = System.currentTimeMillis()
)

// ==================== Recipe Entity to Domain Mappers ====================

fun RecipeEntity.toDomain(): Recipe {
    val ingredientListType = object : TypeToken<List<IngredientDto>>() {}.type
    val instructionListType = object : TypeToken<List<InstructionDto>>() {}.type
    val nutritionType = object : TypeToken<NutritionDto>() {}.type

    val ingredientDtos: List<IngredientDto> = try {
        gson.fromJson(ingredients, ingredientListType) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    val instructionDtos: List<InstructionDto> = try {
        gson.fromJson(instructions, instructionListType) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    val nutritionDto: NutritionDto? = try {
        nutritionInfo?.let { gson.fromJson(it, nutritionType) }
    } catch (e: Exception) {
        null
    }

    return Recipe(
        id = id,
        name = name,
        description = description,
        imageUrl = imageUrl,
        prepTimeMinutes = prepTimeMinutes,
        cookTimeMinutes = cookTimeMinutes,
        servings = servings,
        difficulty = Difficulty.fromValue(difficulty),
        cuisineType = CuisineType.fromValue(cuisineType),
        mealTypes = mealTypes.mapNotNull { MealType.fromValue(it) },
        dietaryTags = dietaryTags.mapNotNull { DietaryTag.fromValue(it) },
        ingredients = ingredientDtos.map { it.toDomainIngredient() },
        instructions = instructionDtos.map { it.toDomainInstruction() },
        nutrition = nutritionDto?.toDomainNutrition(),
        isFavorite = isFavorite
    )
}

// Helper mappers for Recipe nested types
private fun IngredientDto.toDomainIngredient(): Ingredient = Ingredient(
    id = id,
    name = name,
    quantity = quantity,
    unit = unit,
    category = IngredientCategory.fromValue(category),
    isOptional = isOptional,
    substituteFor = substituteFor
)

private fun InstructionDto.toDomainInstruction(): Instruction = Instruction(
    stepNumber = stepNumber,
    instruction = instruction,
    durationMinutes = durationMinutes,
    timerRequired = timerRequired,
    tips = tips
)

private fun NutritionDto.toDomainNutrition(): Nutrition = Nutrition(
    calories = calories,
    proteinGrams = protein,
    carbohydratesGrams = carbohydrates,
    fatGrams = fat,
    fiberGrams = fiber,
    sugarGrams = sugar,
    sodiumMg = sodium
)

// ==================== Grocery Entity Mappers ====================

fun GroceryItemEntity.toDomain(): GroceryItem = GroceryItem(
    id = id,
    name = name,
    quantity = quantity,
    unit = unit,
    category = IngredientCategory.fromValue(category),
    isPurchased = isChecked,
    recipeIds = recipeIds,
    isCustom = mealPlanId == null
)

fun GroceryItem.toEntity(mealPlanId: String?): GroceryItemEntity = GroceryItemEntity(
    id = id,
    name = name,
    quantity = quantity,
    unit = unit,
    category = category.value,
    isChecked = isPurchased,
    mealPlanId = mealPlanId,
    recipeIds = recipeIds,
    notes = null,
    createdAt = System.currentTimeMillis()
)

fun List<GroceryItemEntity>.toGroceryList(
    mealPlanId: String,
    weekStartDate: LocalDate,
    weekEndDate: LocalDate
): GroceryList = GroceryList(
    id = "grocery-$mealPlanId",
    weekStartDate = weekStartDate,
    weekEndDate = weekEndDate,
    items = this.map { it.toDomain() },
    mealPlanId = mealPlanId
)

// ==================== Pantry Item Entity Mappers ====================

fun PantryItemEntity.toDomain(): PantryItem = PantryItem(
    id = id,
    name = name,
    category = PantryCategory.entries.find { it.name == category } ?: PantryCategory.OTHER,
    quantity = quantity,
    unit = unit,
    addedDate = LocalDate.parse(addedDate, DateTimeFormatter.ISO_DATE),
    expiryDate = expiryDate?.let { LocalDate.parse(it, DateTimeFormatter.ISO_DATE) },
    imageUrl = imageUrl
)

fun PantryItem.toEntity(): PantryItemEntity = PantryItemEntity(
    id = id,
    name = name,
    category = category.name,
    quantity = quantity,
    unit = unit,
    addedDate = addedDate.format(DateTimeFormatter.ISO_DATE),
    expiryDate = expiryDate?.format(DateTimeFormatter.ISO_DATE),
    imageUrl = imageUrl
)

// ==================== Favorite Collection Entity Mappers ====================

fun FavoriteCollectionEntity.toDomain(recipeIds: List<String> = emptyList()): FavoriteCollection =
    FavoriteCollection(
        id = id,
        name = name,
        recipeIds = recipeIds,
        coverImageUrl = coverImageUrl,
        isDefault = isDefault,
        createdAt = createdAt
    )

fun FavoriteCollection.toEntity(): FavoriteCollectionEntity =
    FavoriteCollectionEntity(
        id = id,
        name = name,
        coverImageUrl = coverImageUrl,
        order = 0,
        isDefault = isDefault,
        createdAt = createdAt
    )

// ==================== Stats Entity Mappers ====================

fun CookingStreakEntity.toDomain(): CookingStreak = CookingStreak(
    currentStreak = currentStreak,
    bestStreak = bestStreak,
    lastCookingDate = lastCookingDate?.let { LocalDate.parse(it, DateTimeFormatter.ISO_DATE) }
)

fun CookingStreak.toEntity(): CookingStreakEntity = CookingStreakEntity(
    id = "user_streak",
    currentStreak = currentStreak,
    bestStreak = bestStreak,
    lastCookingDate = lastCookingDate?.format(DateTimeFormatter.ISO_DATE)
)

fun CookingDayEntity.toDomain(): CookingDay = CookingDay(
    date = LocalDate.parse(date, DateTimeFormatter.ISO_DATE),
    didCook = didCook,
    mealsCount = mealsCount
)

fun CookingDay.toEntity(): CookingDayEntity = CookingDayEntity(
    date = date.format(DateTimeFormatter.ISO_DATE),
    didCook = didCook,
    mealsCount = mealsCount
)

fun AchievementEntity.toDomain(): Achievement = Achievement(
    id = id,
    name = name,
    description = description,
    emoji = emoji,
    isUnlocked = isUnlocked,
    unlockedDate = unlockedDate?.let { LocalDate.parse(it, DateTimeFormatter.ISO_DATE) }
)

fun Achievement.toEntity(): AchievementEntity = AchievementEntity(
    id = id,
    name = name,
    description = description,
    emoji = emoji,
    isUnlocked = isUnlocked,
    unlockedDate = unlockedDate?.format(DateTimeFormatter.ISO_DATE)
)

fun WeeklyChallengeEntity.toDomain(): WeeklyChallenge = WeeklyChallenge(
    id = id,
    name = name,
    description = description,
    targetCount = targetCount,
    currentProgress = currentProgress,
    rewardBadge = rewardBadge,
    isJoined = isJoined
)

fun WeeklyChallenge.toEntity(weekStartDate: LocalDate, weekEndDate: LocalDate): WeeklyChallengeEntity =
    WeeklyChallengeEntity(
        id = id,
        name = name,
        description = description,
        targetCount = targetCount,
        currentProgress = currentProgress,
        rewardBadge = rewardBadge,
        isJoined = isJoined,
        weekStartDate = weekStartDate.format(DateTimeFormatter.ISO_DATE),
        weekEndDate = weekEndDate.format(DateTimeFormatter.ISO_DATE)
    )

// ==================== Recipe Rules Entity Mappers ====================

private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

fun RecipeRuleEntity.toDomain(): RecipeRule {
    val frequencyDaysList = frequencyDays?.split(",")
        ?.mapNotNull { value ->
            try {
                DayOfWeek.valueOf(value.trim().uppercase())
            } catch (e: Exception) {
                null
            }
        }

    val mealSlotsList = mealSlots?.split(",")
        ?.mapNotNull { value ->
            try {
                val trimmed = value.trim()
                if (trimmed.isNotEmpty()) com.rasoiai.domain.model.MealType.fromValue(trimmed) else null
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()

    return RecipeRule(
        id = id,
        type = RuleType.fromValue(type),
        action = RuleAction.fromValue(action),
        targetId = targetId,
        targetName = targetName,
        frequency = RuleFrequency(
            type = FrequencyType.fromValue(frequencyType),
            count = frequencyCount,
            specificDays = frequencyDaysList
        ),
        enforcement = RuleEnforcement.fromValue(enforcement),
        mealSlots = mealSlotsList,
        isActive = isActive,
        createdAt = LocalDateTime.parse(createdAt, dateTimeFormatter),
        updatedAt = LocalDateTime.parse(updatedAt, dateTimeFormatter)
    )
}

fun RecipeRule.toEntity(syncStatus: String = "SYNCED"): RecipeRuleEntity = RecipeRuleEntity(
    id = id,
    type = type.value,
    action = action.value,
    targetId = targetId,
    targetName = targetName,
    frequencyType = frequency.type.value,
    frequencyCount = frequency.count,
    frequencyDays = frequency.specificDays?.joinToString(",") { it.name },
    enforcement = enforcement.value,
    mealSlots = if (mealSlots.isNotEmpty()) mealSlots.joinToString(",") { it.value } else null,
    isActive = isActive,
    syncStatus = syncStatus,
    createdAt = createdAt.format(dateTimeFormatter),
    updatedAt = updatedAt.format(dateTimeFormatter)
)

fun NutritionGoalEntity.toDomain(): NutritionGoal = NutritionGoal(
    id = id,
    foodCategory = FoodCategory.fromValue(foodCategory),
    weeklyTarget = weeklyTarget,
    currentProgress = currentProgress,
    enforcement = RuleEnforcement.fromValue(enforcement),
    isActive = isActive,
    createdAt = LocalDateTime.parse(createdAt, dateTimeFormatter),
    updatedAt = LocalDateTime.parse(updatedAt, dateTimeFormatter)
)

fun NutritionGoal.toEntity(syncStatus: String = "SYNCED"): NutritionGoalEntity = NutritionGoalEntity(
    id = id,
    foodCategory = foodCategory.value,
    weeklyTarget = weeklyTarget,
    currentProgress = currentProgress,
    enforcement = enforcement.value,
    isActive = isActive,
    syncStatus = syncStatus,
    createdAt = createdAt.format(dateTimeFormatter),
    updatedAt = updatedAt.format(dateTimeFormatter)
)

// ==================== Chat Message Entity Mappers ====================

fun ChatMessageEntity.toDomain(): ChatMessage {
    val quickActionListType = object : TypeToken<List<String>>() {}.type
    val recipeSuggestionListType = object : TypeToken<List<RecipeSuggestion>>() {}.type

    val quickActionsList: List<String>? = try {
        quickActionsJson?.let { gson.fromJson(it, quickActionListType) }
    } catch (e: Exception) {
        null
    }

    val recipeSuggestionsList: List<RecipeSuggestion>? = try {
        recipeSuggestionsJson?.let { gson.fromJson(it, recipeSuggestionListType) }
    } catch (e: Exception) {
        null
    }

    return ChatMessage(
        id = id,
        content = content,
        isFromUser = isFromUser,
        timestamp = timestamp,
        quickActions = quickActionsList,
        recipeSuggestions = recipeSuggestionsList
    )
}

fun ChatMessage.toEntity(): ChatMessageEntity = ChatMessageEntity(
    id = id,
    content = content,
    isFromUser = isFromUser,
    timestamp = timestamp,
    quickActionsJson = quickActions?.let { gson.toJson(it) },
    recipeSuggestionsJson = recipeSuggestions?.let { gson.toJson(it) }
)

// ==================== Notification Entity Mappers ====================

fun NotificationEntity.toDomain(): Notification {
    val actionDataParsed = actionData?.let {
        try {
            gson.fromJson(it, NotificationActionData::class.java)
        } catch (e: Exception) {
            null
        }
    }

    return Notification(
        id = id,
        type = NotificationType.fromValue(type),
        title = title,
        body = body,
        imageUrl = imageUrl,
        actionType = NotificationActionType.fromValue(actionType),
        actionData = actionDataParsed,
        isRead = isRead,
        createdAt = createdAt,
        expiresAt = expiresAt
    )
}

fun Notification.toEntity(): NotificationEntity = NotificationEntity(
    id = id,
    type = type.value,
    title = title,
    body = body,
    imageUrl = imageUrl,
    actionType = actionType.value,
    actionData = actionData?.let { gson.toJson(it) },
    isRead = isRead,
    createdAt = createdAt,
    expiresAt = expiresAt
)

fun NotificationDto.toEntity(): NotificationEntity {
    val actionDataJson = actionData?.let { gson.toJson(it) }

    return NotificationEntity(
        id = id,
        type = type,
        title = title,
        body = body,
        imageUrl = imageUrl,
        actionType = actionType,
        actionData = actionDataJson,
        isRead = isRead,
        createdAt = parseTimestampToMillis(createdAt),
        expiresAt = expiresAt?.let { parseTimestampToMillis(it) }
    )
}

fun NotificationDto.toDomain(): Notification {
    val actionDataParsed = actionData?.let {
        NotificationActionData(
            recipeId = it.recipeId,
            mealPlanId = it.mealPlanId,
            festivalId = it.festivalId,
            streakCount = it.streakCount
        )
    }

    return Notification(
        id = id,
        type = NotificationType.fromValue(type),
        title = title,
        body = body,
        imageUrl = imageUrl,
        actionType = NotificationActionType.fromValue(actionType),
        actionData = actionDataParsed,
        isRead = isRead,
        createdAt = parseTimestampToMillis(createdAt),
        expiresAt = expiresAt?.let { parseTimestampToMillis(it) }
    )
}

// ==================== Offline Queue Entity Mappers ====================

fun OfflineQueueEntity.toDomain(): OfflineAction = OfflineAction(
    id = id,
    actionType = OfflineActionType.fromValue(actionType),
    payload = payload,
    status = ActionStatus.fromValue(status),
    retryCount = retryCount,
    errorMessage = errorMessage,
    createdAt = createdAt,
    lastAttemptAt = lastAttemptAt
)

fun OfflineAction.toEntity(): OfflineQueueEntity = OfflineQueueEntity(
    id = id,
    actionType = actionType.value,
    payload = payload,
    status = status.value,
    retryCount = retryCount,
    errorMessage = errorMessage,
    createdAt = createdAt,
    lastAttemptAt = lastAttemptAt
)

// ==================== Helper Functions ====================

private fun parseTimestampToMillis(timestamp: String): Long {
    return try {
        java.time.ZonedDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME)
            .toInstant().toEpochMilli()
    } catch (e: Exception) {
        try {
            java.time.OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .toInstant().toEpochMilli()
        } catch (e2: Exception) {
            try {
                LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
            } catch (e3: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}

private fun parseTimestamp(timestamp: String): Long {
    return try {
        java.time.ZonedDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME)
            .toInstant().toEpochMilli()
    } catch (e: Exception) {
        try {
            LocalDate.parse(timestamp, DateTimeFormatter.ISO_DATE)
                .atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (e2: Exception) {
            System.currentTimeMillis()
        }
    }
}

// ==================== Recipe Rule DTO Mappers ====================

/**
 * Convert API DTO to Room Entity.
 */
fun RecipeRuleDto.toEntity(): RecipeRuleEntity = RecipeRuleEntity(
    id = id,
    type = targetType, // API uses targetType, Room uses type
    action = action,
    targetId = targetId ?: "",
    targetName = targetName,
    frequencyType = frequencyType,
    frequencyCount = frequencyCount,
    frequencyDays = frequencyDays,
    enforcement = enforcement,
    mealSlots = mealSlot, // API sends single meal_slot, stored as comma-separated mealSlots
    isActive = isActive,
    syncStatus = syncStatus,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * Convert Room Entity to API sync item.
 */
fun RecipeRuleEntity.toSyncItem(): RecipeRuleSyncItem = RecipeRuleSyncItem(
    id = id,
    targetType = type, // Room uses type, API uses targetType
    action = action,
    targetId = targetId.takeIf { it.isNotEmpty() },
    targetName = targetName,
    frequencyType = frequencyType,
    frequencyCount = frequencyCount,
    frequencyDays = frequencyDays,
    enforcement = enforcement,
    mealSlot = mealSlots, // Entity stores as mealSlots, API expects meal_slot
    isActive = isActive,
    localUpdatedAt = updatedAt
)

// ==================== Nutrition Goal DTO Mappers ====================

/**
 * Convert API DTO to Room Entity.
 */
fun NutritionGoalDto.toEntity(): NutritionGoalEntity = NutritionGoalEntity(
    id = id,
    foodCategory = foodCategory,
    weeklyTarget = weeklyTarget,
    currentProgress = currentProgress,
    enforcement = enforcement,
    isActive = isActive,
    syncStatus = syncStatus,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * Convert Room Entity to API sync item.
 */
fun NutritionGoalEntity.toSyncItem(): NutritionGoalSyncItem = NutritionGoalSyncItem(
    id = id,
    foodCategory = foodCategory,
    weeklyTarget = weeklyTarget,
    currentProgress = currentProgress,
    enforcement = enforcement,
    isActive = isActive,
    localUpdatedAt = updatedAt
)

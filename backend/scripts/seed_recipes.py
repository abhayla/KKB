"""Seed script for initial recipe data."""

import asyncio
import uuid

from sqlalchemy import select

from app.db.database import async_session_maker
from app.models.recipe import Recipe, RecipeIngredient, RecipeInstruction, RecipeNutrition

# Sample Indian recipes covering all cuisines and meal types
RECIPES = [
    # North Indian Breakfast
    {
        "name": "Aloo Paratha",
        "description": "Crispy whole wheat flatbread stuffed with spiced mashed potatoes, a classic North Indian breakfast.",
        "cuisine_type": "north",
        "meal_types": ["breakfast"],
        "dietary_tags": ["vegetarian"],
        "prep_time_minutes": 20,
        "cook_time_minutes": 20,
        "servings": 4,
        "difficulty_level": "medium",
        "ingredients": [
            {"name": "Whole wheat flour", "quantity": 2, "unit": "cups", "category": "grains"},
            {"name": "Potatoes", "quantity": 3, "unit": "medium", "category": "vegetables"},
            {"name": "Green chilies", "quantity": 2, "unit": "pieces", "category": "vegetables"},
            {"name": "Cumin seeds", "quantity": 1, "unit": "tsp", "category": "spices"},
            {"name": "Coriander leaves", "quantity": 0.25, "unit": "cups", "category": "vegetables"},
            {"name": "Ghee", "quantity": 4, "unit": "tbsp", "category": "dairy"},
        ],
        "instructions": [
            {"step": 1, "text": "Boil and mash potatoes. Mix with green chilies, cumin, coriander, and salt.", "time": 15},
            {"step": 2, "text": "Knead wheat flour with water to make soft dough. Rest for 10 minutes.", "time": 10},
            {"step": 3, "text": "Roll dough into circles, place potato filling, seal and roll again.", "time": 10},
            {"step": 4, "text": "Cook on hot tawa with ghee until golden brown on both sides.", "time": 5},
        ],
        "nutrition": {"calories": 280, "protein": 7, "carbs": 42, "fat": 10, "fiber": 4},
    },
    {
        "name": "Poha",
        "description": "Light and fluffy flattened rice with peanuts, onions, and mild spices.",
        "cuisine_type": "west",
        "meal_types": ["breakfast", "snacks"],
        "dietary_tags": ["vegetarian", "vegan"],
        "prep_time_minutes": 10,
        "cook_time_minutes": 15,
        "servings": 4,
        "difficulty_level": "easy",
        "ingredients": [
            {"name": "Flattened rice (poha)", "quantity": 2, "unit": "cups", "category": "grains"},
            {"name": "Onion", "quantity": 1, "unit": "medium", "category": "vegetables"},
            {"name": "Peanuts", "quantity": 0.25, "unit": "cups", "category": "nuts"},
            {"name": "Mustard seeds", "quantity": 1, "unit": "tsp", "category": "spices"},
            {"name": "Turmeric", "quantity": 0.5, "unit": "tsp", "category": "spices"},
            {"name": "Curry leaves", "quantity": 8, "unit": "pieces", "category": "spices"},
        ],
        "instructions": [
            {"step": 1, "text": "Rinse poha gently and drain. Set aside.", "time": 3},
            {"step": 2, "text": "Heat oil, add mustard seeds, peanuts, curry leaves.", "time": 3},
            {"step": 3, "text": "Add onions and saute until translucent.", "time": 5},
            {"step": 4, "text": "Add turmeric, poha, salt. Mix gently and cook covered.", "time": 5},
        ],
        "nutrition": {"calories": 220, "protein": 6, "carbs": 38, "fat": 6, "fiber": 2},
    },
    {
        "name": "Idli Sambhar",
        "description": "Soft steamed rice cakes served with flavorful lentil vegetable stew.",
        "cuisine_type": "south",
        "meal_types": ["breakfast"],
        "dietary_tags": ["vegetarian", "vegan"],
        "prep_time_minutes": 30,
        "cook_time_minutes": 30,
        "servings": 4,
        "difficulty_level": "medium",
        "is_kid_friendly": True,
        "ingredients": [
            {"name": "Idli rice", "quantity": 2, "unit": "cups", "category": "grains"},
            {"name": "Urad dal", "quantity": 0.5, "unit": "cups", "category": "pulses"},
            {"name": "Toor dal", "quantity": 0.5, "unit": "cups", "category": "pulses"},
            {"name": "Mixed vegetables", "quantity": 1, "unit": "cups", "category": "vegetables"},
            {"name": "Sambhar powder", "quantity": 2, "unit": "tbsp", "category": "spices"},
            {"name": "Tamarind", "quantity": 1, "unit": "tbsp", "category": "other"},
        ],
        "instructions": [
            {"step": 1, "text": "Soak rice and dal separately for 4-6 hours. Grind to smooth batter.", "time": 20},
            {"step": 2, "text": "Ferment batter overnight. Pour into idli molds.", "time": 5},
            {"step": 3, "text": "Steam idlis for 10-12 minutes until done.", "time": 12, "timer": True},
            {"step": 4, "text": "Cook dal with vegetables, add sambhar powder and tamarind.", "time": 20},
        ],
        "nutrition": {"calories": 250, "protein": 9, "carbs": 45, "fat": 3, "fiber": 5},
    },
    {
        "name": "Masala Dosa",
        "description": "Crispy fermented rice crepe filled with spiced potato masala.",
        "cuisine_type": "south",
        "meal_types": ["breakfast", "dinner"],
        "dietary_tags": ["vegetarian", "vegan"],
        "prep_time_minutes": 30,
        "cook_time_minutes": 25,
        "servings": 4,
        "difficulty_level": "medium",
        "ingredients": [
            {"name": "Dosa rice", "quantity": 2, "unit": "cups", "category": "grains"},
            {"name": "Urad dal", "quantity": 0.5, "unit": "cups", "category": "pulses"},
            {"name": "Potatoes", "quantity": 4, "unit": "medium", "category": "vegetables"},
            {"name": "Onion", "quantity": 2, "unit": "medium", "category": "vegetables"},
            {"name": "Mustard seeds", "quantity": 1, "unit": "tsp", "category": "spices"},
            {"name": "Turmeric", "quantity": 0.5, "unit": "tsp", "category": "spices"},
        ],
        "instructions": [
            {"step": 1, "text": "Soak rice and dal, grind to smooth batter, ferment overnight.", "time": 15},
            {"step": 2, "text": "Boil potatoes, mash roughly. Prepare tempering with mustard.", "time": 15},
            {"step": 3, "text": "Add onions, turmeric, and mashed potatoes to tempering.", "time": 10},
            {"step": 4, "text": "Spread batter thin on hot tawa, add potato filling, fold.", "time": 10},
        ],
        "nutrition": {"calories": 320, "protein": 8, "carbs": 55, "fat": 8, "fiber": 4},
    },
    {
        "name": "Dal Makhani",
        "description": "Creamy black lentils slow-cooked with butter and cream.",
        "cuisine_type": "north",
        "meal_types": ["lunch", "dinner"],
        "dietary_tags": ["vegetarian"],
        "prep_time_minutes": 20,
        "cook_time_minutes": 60,
        "servings": 6,
        "difficulty_level": "medium",
        "ingredients": [
            {"name": "Black urad dal", "quantity": 1, "unit": "cups", "category": "pulses"},
            {"name": "Rajma", "quantity": 0.25, "unit": "cups", "category": "pulses"},
            {"name": "Butter", "quantity": 4, "unit": "tbsp", "category": "dairy"},
            {"name": "Cream", "quantity": 0.5, "unit": "cups", "category": "dairy"},
            {"name": "Tomato puree", "quantity": 1, "unit": "cups", "category": "vegetables"},
            {"name": "Ginger garlic paste", "quantity": 2, "unit": "tbsp", "category": "spices"},
        ],
        "instructions": [
            {"step": 1, "text": "Soak dal and rajma overnight. Pressure cook until soft.", "time": 30},
            {"step": 2, "text": "In butter, saute ginger garlic paste and tomato puree.", "time": 10},
            {"step": 3, "text": "Add cooked dal, simmer on low heat for 30 minutes.", "time": 30, "timer": True},
            {"step": 4, "text": "Add cream, butter, adjust salt. Simmer 10 more minutes.", "time": 10},
        ],
        "nutrition": {"calories": 350, "protein": 14, "carbs": 35, "fat": 18, "fiber": 8},
    },
    {
        "name": "Paneer Butter Masala",
        "description": "Soft paneer cubes in rich, creamy tomato gravy.",
        "cuisine_type": "north",
        "meal_types": ["lunch", "dinner"],
        "dietary_tags": ["vegetarian"],
        "prep_time_minutes": 15,
        "cook_time_minutes": 25,
        "servings": 4,
        "difficulty_level": "easy",
        "is_kid_friendly": True,
        "ingredients": [
            {"name": "Paneer", "quantity": 250, "unit": "grams", "category": "dairy"},
            {"name": "Tomatoes", "quantity": 4, "unit": "medium", "category": "vegetables"},
            {"name": "Butter", "quantity": 3, "unit": "tbsp", "category": "dairy"},
            {"name": "Cream", "quantity": 0.25, "unit": "cups", "category": "dairy"},
            {"name": "Kashmiri red chili", "quantity": 1, "unit": "tsp", "category": "spices"},
            {"name": "Garam masala", "quantity": 1, "unit": "tsp", "category": "spices"},
        ],
        "instructions": [
            {"step": 1, "text": "Blanch tomatoes, blend to smooth puree.", "time": 10},
            {"step": 2, "text": "Heat butter, add tomato puree, cook until oil separates.", "time": 12},
            {"step": 3, "text": "Add spices, cream, and paneer cubes.", "time": 8},
            {"step": 4, "text": "Simmer for 5 minutes. Garnish with cream and coriander.", "time": 5},
        ],
        "nutrition": {"calories": 380, "protein": 16, "carbs": 12, "fat": 32, "fiber": 2},
    },
    {
        "name": "Vegetable Biryani",
        "description": "Fragrant basmati rice layered with mixed vegetables and spices.",
        "cuisine_type": "south",
        "meal_types": ["lunch", "dinner"],
        "dietary_tags": ["vegetarian"],
        "prep_time_minutes": 30,
        "cook_time_minutes": 40,
        "servings": 6,
        "difficulty_level": "medium",
        "ingredients": [
            {"name": "Basmati rice", "quantity": 2, "unit": "cups", "category": "grains"},
            {"name": "Mixed vegetables", "quantity": 2, "unit": "cups", "category": "vegetables"},
            {"name": "Yogurt", "quantity": 0.5, "unit": "cups", "category": "dairy"},
            {"name": "Biryani masala", "quantity": 2, "unit": "tbsp", "category": "spices"},
            {"name": "Saffron", "quantity": 1, "unit": "pinch", "category": "spices"},
            {"name": "Fried onions", "quantity": 0.5, "unit": "cups", "category": "vegetables"},
        ],
        "instructions": [
            {"step": 1, "text": "Soak rice for 30 minutes. Parboil until 70% done.", "time": 15},
            {"step": 2, "text": "Marinate vegetables in yogurt and biryani masala.", "time": 15},
            {"step": 3, "text": "Layer vegetables and rice in pot. Add saffron milk.", "time": 10},
            {"step": 4, "text": "Seal with dough, cook on dum for 25 minutes.", "time": 25, "timer": True},
        ],
        "nutrition": {"calories": 380, "protein": 10, "carbs": 68, "fat": 8, "fiber": 5},
    },
    {
        "name": "Pav Bhaji",
        "description": "Spiced mashed vegetable curry served with butter-toasted bread rolls.",
        "cuisine_type": "west",
        "meal_types": ["dinner", "snacks"],
        "dietary_tags": ["vegetarian"],
        "prep_time_minutes": 20,
        "cook_time_minutes": 30,
        "servings": 4,
        "difficulty_level": "easy",
        "is_kid_friendly": True,
        "ingredients": [
            {"name": "Mixed vegetables", "quantity": 3, "unit": "cups", "category": "vegetables"},
            {"name": "Pav (bread rolls)", "quantity": 8, "unit": "pieces", "category": "grains"},
            {"name": "Butter", "quantity": 6, "unit": "tbsp", "category": "dairy"},
            {"name": "Pav bhaji masala", "quantity": 3, "unit": "tbsp", "category": "spices"},
            {"name": "Onion", "quantity": 2, "unit": "medium", "category": "vegetables"},
            {"name": "Tomatoes", "quantity": 3, "unit": "medium", "category": "vegetables"},
        ],
        "instructions": [
            {"step": 1, "text": "Boil and mash mixed vegetables (potato, cauliflower, peas).", "time": 20},
            {"step": 2, "text": "Saute onions and tomatoes in butter until soft.", "time": 10},
            {"step": 3, "text": "Add mashed vegetables, pav bhaji masala, mix well.", "time": 10},
            {"step": 4, "text": "Toast pav with butter. Serve hot with bhaji.", "time": 5},
        ],
        "nutrition": {"calories": 420, "protein": 10, "carbs": 55, "fat": 20, "fiber": 6},
    },
    {
        "name": "Samosa",
        "description": "Crispy triangular pastries filled with spiced potatoes and peas.",
        "cuisine_type": "north",
        "meal_types": ["snacks"],
        "dietary_tags": ["vegetarian", "vegan"],
        "prep_time_minutes": 40,
        "cook_time_minutes": 20,
        "servings": 8,
        "difficulty_level": "hard",
        "ingredients": [
            {"name": "All purpose flour", "quantity": 2, "unit": "cups", "category": "grains"},
            {"name": "Potatoes", "quantity": 4, "unit": "medium", "category": "vegetables"},
            {"name": "Green peas", "quantity": 0.5, "unit": "cups", "category": "vegetables"},
            {"name": "Cumin seeds", "quantity": 1, "unit": "tsp", "category": "spices"},
            {"name": "Garam masala", "quantity": 1, "unit": "tsp", "category": "spices"},
            {"name": "Oil for frying", "quantity": 2, "unit": "cups", "category": "oils"},
        ],
        "instructions": [
            {"step": 1, "text": "Make stiff dough with flour and oil. Rest for 30 minutes.", "time": 10},
            {"step": 2, "text": "Boil and mash potatoes. Mix with peas and spices.", "time": 15},
            {"step": 3, "text": "Roll dough, cut semicircles, form cones, fill and seal.", "time": 20},
            {"step": 4, "text": "Deep fry on medium heat until golden and crispy.", "time": 15},
        ],
        "nutrition": {"calories": 180, "protein": 4, "carbs": 28, "fat": 7, "fiber": 2},
    },
    {
        "name": "Jeera Rice",
        "description": "Fragrant basmati rice tempered with cumin seeds.",
        "cuisine_type": "north",
        "meal_types": ["lunch", "dinner"],
        "dietary_tags": ["vegetarian", "vegan"],
        "prep_time_minutes": 5,
        "cook_time_minutes": 20,
        "servings": 4,
        "difficulty_level": "easy",
        "is_quick_meal": True,
        "ingredients": [
            {"name": "Basmati rice", "quantity": 1.5, "unit": "cups", "category": "grains"},
            {"name": "Cumin seeds", "quantity": 1.5, "unit": "tsp", "category": "spices"},
            {"name": "Ghee", "quantity": 2, "unit": "tbsp", "category": "dairy"},
            {"name": "Bay leaf", "quantity": 1, "unit": "piece", "category": "spices"},
            {"name": "Salt", "quantity": 1, "unit": "tsp", "category": "other"},
        ],
        "instructions": [
            {"step": 1, "text": "Wash and soak rice for 20 minutes. Drain.", "time": 5},
            {"step": 2, "text": "Heat ghee, add cumin seeds and bay leaf.", "time": 2},
            {"step": 3, "text": "Add rice, saute for 2 minutes.", "time": 2},
            {"step": 4, "text": "Add water (1:1.5 ratio), salt. Cook until done.", "time": 15, "timer": True},
        ],
        "nutrition": {"calories": 220, "protein": 4, "carbs": 40, "fat": 5, "fiber": 1},
    },
    {
        "name": "Rajma Chawal",
        "description": "Hearty kidney bean curry served over steamed rice.",
        "cuisine_type": "north",
        "meal_types": ["lunch", "dinner"],
        "dietary_tags": ["vegetarian", "vegan"],
        "prep_time_minutes": 20,
        "cook_time_minutes": 45,
        "servings": 4,
        "difficulty_level": "easy",
        "is_kid_friendly": True,
        "ingredients": [
            {"name": "Kidney beans (rajma)", "quantity": 1.5, "unit": "cups", "category": "pulses"},
            {"name": "Basmati rice", "quantity": 1.5, "unit": "cups", "category": "grains"},
            {"name": "Onion", "quantity": 2, "unit": "medium", "category": "vegetables"},
            {"name": "Tomatoes", "quantity": 3, "unit": "medium", "category": "vegetables"},
            {"name": "Rajma masala", "quantity": 2, "unit": "tbsp", "category": "spices"},
            {"name": "Ginger garlic paste", "quantity": 1, "unit": "tbsp", "category": "spices"},
        ],
        "instructions": [
            {"step": 1, "text": "Soak rajma overnight. Pressure cook until soft.", "time": 30},
            {"step": 2, "text": "Saute onions, ginger garlic, add tomatoes.", "time": 10},
            {"step": 3, "text": "Add rajma masala, cooked rajma with liquid. Simmer.", "time": 20},
            {"step": 4, "text": "Cook rice separately. Serve rajma over rice.", "time": 15},
        ],
        "nutrition": {"calories": 380, "protein": 14, "carbs": 68, "fat": 5, "fiber": 12},
    },
    {
        "name": "Palak Paneer",
        "description": "Creamy spinach curry with soft paneer cubes.",
        "cuisine_type": "north",
        "meal_types": ["lunch", "dinner"],
        "dietary_tags": ["vegetarian"],
        "prep_time_minutes": 20,
        "cook_time_minutes": 25,
        "servings": 4,
        "difficulty_level": "medium",
        "ingredients": [
            {"name": "Spinach", "quantity": 500, "unit": "grams", "category": "vegetables"},
            {"name": "Paneer", "quantity": 200, "unit": "grams", "category": "dairy"},
            {"name": "Onion", "quantity": 1, "unit": "medium", "category": "vegetables"},
            {"name": "Cream", "quantity": 2, "unit": "tbsp", "category": "dairy"},
            {"name": "Cumin seeds", "quantity": 1, "unit": "tsp", "category": "spices"},
            {"name": "Green chilies", "quantity": 2, "unit": "pieces", "category": "vegetables"},
        ],
        "instructions": [
            {"step": 1, "text": "Blanch spinach in boiling water. Blend to puree.", "time": 10},
            {"step": 2, "text": "Saute onions, add spinach puree and spices.", "time": 10},
            {"step": 3, "text": "Add paneer cubes, simmer for 5 minutes.", "time": 8},
            {"step": 4, "text": "Add cream, adjust seasoning. Serve hot.", "time": 5},
        ],
        "nutrition": {"calories": 280, "protein": 16, "carbs": 10, "fat": 20, "fiber": 4},
    },
    {
        "name": "Upma",
        "description": "Savory semolina breakfast dish with vegetables.",
        "cuisine_type": "south",
        "meal_types": ["breakfast"],
        "dietary_tags": ["vegetarian"],
        "prep_time_minutes": 10,
        "cook_time_minutes": 15,
        "servings": 4,
        "difficulty_level": "easy",
        "is_quick_meal": True,
        "ingredients": [
            {"name": "Semolina (rava)", "quantity": 1, "unit": "cups", "category": "grains"},
            {"name": "Onion", "quantity": 1, "unit": "medium", "category": "vegetables"},
            {"name": "Mustard seeds", "quantity": 1, "unit": "tsp", "category": "spices"},
            {"name": "Curry leaves", "quantity": 8, "unit": "pieces", "category": "spices"},
            {"name": "Cashews", "quantity": 10, "unit": "pieces", "category": "nuts"},
            {"name": "Green chilies", "quantity": 2, "unit": "pieces", "category": "vegetables"},
        ],
        "instructions": [
            {"step": 1, "text": "Dry roast semolina until fragrant. Set aside.", "time": 5},
            {"step": 2, "text": "Temper mustard seeds, add onions and cashews.", "time": 5},
            {"step": 3, "text": "Add water, bring to boil.", "time": 3},
            {"step": 4, "text": "Slowly add roasted semolina, stirring constantly.", "time": 5},
        ],
        "nutrition": {"calories": 240, "protein": 6, "carbs": 38, "fat": 8, "fiber": 2},
    },
    {
        "name": "Besan Chilla",
        "description": "Savory gram flour pancakes with vegetables.",
        "cuisine_type": "north",
        "meal_types": ["breakfast", "snacks"],
        "dietary_tags": ["vegetarian", "vegan"],
        "prep_time_minutes": 10,
        "cook_time_minutes": 15,
        "servings": 4,
        "difficulty_level": "easy",
        "is_quick_meal": True,
        "ingredients": [
            {"name": "Gram flour (besan)", "quantity": 1, "unit": "cups", "category": "grains"},
            {"name": "Onion", "quantity": 1, "unit": "small", "category": "vegetables"},
            {"name": "Tomato", "quantity": 1, "unit": "small", "category": "vegetables"},
            {"name": "Green chilies", "quantity": 2, "unit": "pieces", "category": "vegetables"},
            {"name": "Coriander leaves", "quantity": 2, "unit": "tbsp", "category": "vegetables"},
            {"name": "Ajwain", "quantity": 0.5, "unit": "tsp", "category": "spices"},
        ],
        "instructions": [
            {"step": 1, "text": "Mix besan with water to make smooth batter.", "time": 3},
            {"step": 2, "text": "Add chopped vegetables, spices, and salt.", "time": 3},
            {"step": 3, "text": "Pour batter on hot tawa, spread thin.", "time": 2},
            {"step": 4, "text": "Cook both sides until golden. Serve with chutney.", "time": 5},
        ],
        "nutrition": {"calories": 160, "protein": 8, "carbs": 22, "fat": 5, "fiber": 4},
    },
    {
        "name": "Chole Bhature",
        "description": "Spicy chickpea curry served with fluffy deep-fried bread.",
        "cuisine_type": "north",
        "meal_types": ["lunch", "breakfast"],
        "dietary_tags": ["vegetarian"],
        "prep_time_minutes": 30,
        "cook_time_minutes": 45,
        "servings": 4,
        "difficulty_level": "medium",
        "ingredients": [
            {"name": "Chickpeas", "quantity": 1.5, "unit": "cups", "category": "pulses"},
            {"name": "All purpose flour", "quantity": 2, "unit": "cups", "category": "grains"},
            {"name": "Onion", "quantity": 2, "unit": "medium", "category": "vegetables"},
            {"name": "Tomatoes", "quantity": 2, "unit": "medium", "category": "vegetables"},
            {"name": "Chole masala", "quantity": 2, "unit": "tbsp", "category": "spices"},
            {"name": "Tea bags", "quantity": 2, "unit": "pieces", "category": "other"},
        ],
        "instructions": [
            {"step": 1, "text": "Soak chickpeas overnight. Boil with tea bags for color.", "time": 30},
            {"step": 2, "text": "Prepare masala with onions, tomatoes, and chole masala.", "time": 15},
            {"step": 3, "text": "Add boiled chickpeas, simmer until thick.", "time": 20},
            {"step": 4, "text": "Knead bhatura dough, rest, roll and deep fry until puffed.", "time": 20},
        ],
        "nutrition": {"calories": 450, "protein": 15, "carbs": 65, "fat": 16, "fiber": 10},
    },
    {
        "name": "Dhokla",
        "description": "Soft, spongy steamed savory cake made from gram flour.",
        "cuisine_type": "west",
        "meal_types": ["snacks", "breakfast"],
        "dietary_tags": ["vegetarian", "vegan"],
        "prep_time_minutes": 15,
        "cook_time_minutes": 20,
        "servings": 6,
        "difficulty_level": "medium",
        "ingredients": [
            {"name": "Gram flour (besan)", "quantity": 1.5, "unit": "cups", "category": "grains"},
            {"name": "Yogurt", "quantity": 0.5, "unit": "cups", "category": "dairy"},
            {"name": "Mustard seeds", "quantity": 1, "unit": "tsp", "category": "spices"},
            {"name": "Green chilies", "quantity": 2, "unit": "pieces", "category": "vegetables"},
            {"name": "Eno fruit salt", "quantity": 1, "unit": "tsp", "category": "other"},
            {"name": "Sugar", "quantity": 2, "unit": "tbsp", "category": "sweeteners"},
        ],
        "instructions": [
            {"step": 1, "text": "Mix gram flour, yogurt, water. Let ferment 4-6 hours.", "time": 5},
            {"step": 2, "text": "Add eno, pour into greased plate immediately.", "time": 2},
            {"step": 3, "text": "Steam for 15-20 minutes until toothpick comes clean.", "time": 18, "timer": True},
            {"step": 4, "text": "Temper with mustard seeds and curry leaves. Cut and serve.", "time": 5},
        ],
        "nutrition": {"calories": 150, "protein": 6, "carbs": 22, "fat": 4, "fiber": 3},
    },
    {
        "name": "Sabudana Khichdi",
        "description": "Tapioca pearls with peanuts and potatoes, popular fasting dish.",
        "cuisine_type": "west",
        "meal_types": ["breakfast", "snacks"],
        "dietary_tags": ["vegetarian"],
        "prep_time_minutes": 30,
        "cook_time_minutes": 15,
        "servings": 4,
        "difficulty_level": "easy",
        "is_fasting_friendly": True,
        "ingredients": [
            {"name": "Sabudana (tapioca)", "quantity": 1, "unit": "cups", "category": "grains"},
            {"name": "Peanuts", "quantity": 0.25, "unit": "cups", "category": "nuts"},
            {"name": "Potatoes", "quantity": 2, "unit": "medium", "category": "vegetables"},
            {"name": "Green chilies", "quantity": 2, "unit": "pieces", "category": "vegetables"},
            {"name": "Cumin seeds", "quantity": 1, "unit": "tsp", "category": "spices"},
            {"name": "Rock salt", "quantity": 1, "unit": "tsp", "category": "other"},
        ],
        "instructions": [
            {"step": 1, "text": "Soak sabudana overnight in just enough water to cover.", "time": 5},
            {"step": 2, "text": "Roast and coarsely grind peanuts.", "time": 5},
            {"step": 3, "text": "Saute cumin, add potatoes, cook until crisp.", "time": 8},
            {"step": 4, "text": "Add soaked sabudana, peanuts. Cook until translucent.", "time": 7},
        ],
        "nutrition": {"calories": 280, "protein": 6, "carbs": 45, "fat": 9, "fiber": 2},
    },
]


async def seed_recipes():
    """Seed the database with sample recipes."""
    async with async_session_maker() as session:
        # Check if recipes already exist
        result = await session.execute(select(Recipe).limit(1))
        if result.scalar_one_or_none():
            print("Recipes already exist. Skipping seed.")
            return

        for recipe_data in RECIPES:
            recipe_id = str(uuid.uuid4())
            # Create recipe
            recipe = Recipe(
                id=recipe_id,
                name=recipe_data["name"],
                description=recipe_data["description"],
                cuisine_type=recipe_data["cuisine_type"],
                meal_types=recipe_data["meal_types"],
                dietary_tags=recipe_data["dietary_tags"],
                prep_time_minutes=recipe_data["prep_time_minutes"],
                cook_time_minutes=recipe_data["cook_time_minutes"],
                total_time_minutes=recipe_data["prep_time_minutes"] + recipe_data["cook_time_minutes"],
                servings=recipe_data["servings"],
                difficulty_level=recipe_data["difficulty_level"],
                is_fasting_friendly=recipe_data.get("is_fasting_friendly", False),
                is_quick_meal=recipe_data.get("is_quick_meal", False),
                is_kid_friendly=recipe_data.get("is_kid_friendly", False),
            )
            session.add(recipe)

            # Add ingredients
            for i, ing_data in enumerate(recipe_data["ingredients"]):
                ingredient = RecipeIngredient(
                    id=str(uuid.uuid4()),
                    recipe_id=recipe_id,
                    name=ing_data["name"],
                    quantity=ing_data["quantity"],
                    unit=ing_data["unit"],
                    category=ing_data["category"],
                    order=i,
                )
                session.add(ingredient)

            # Add instructions
            for inst_data in recipe_data["instructions"]:
                instruction = RecipeInstruction(
                    id=str(uuid.uuid4()),
                    recipe_id=recipe_id,
                    step_number=inst_data["step"],
                    instruction=inst_data["text"],
                    duration_minutes=inst_data.get("time"),
                    timer_required=inst_data.get("timer", False),
                )
                session.add(instruction)

            # Add nutrition
            if "nutrition" in recipe_data:
                nutr = recipe_data["nutrition"]
                nutrition = RecipeNutrition(
                    id=str(uuid.uuid4()),
                    recipe_id=recipe_id,
                    calories=nutr["calories"],
                    protein_grams=nutr["protein"],
                    carbohydrates_grams=nutr["carbs"],
                    fat_grams=nutr["fat"],
                    fiber_grams=nutr["fiber"],
                )
                session.add(nutrition)

        await session.commit()
        print(f"Seeded {len(RECIPES)} recipes successfully!")


if __name__ == "__main__":
    asyncio.run(seed_recipes())

"""Seed script for Indian festival data."""

import asyncio
import uuid
from datetime import date

from sqlalchemy import select

from app.db.database import async_session_maker
from app.models.festival import Festival

# Indian festivals for 2025
FESTIVALS = [
    {
        "name": "Makar Sankranti",
        "name_hindi": "मकर संक्रांति",
        "date": date(2025, 1, 14),
        "regions": ["north", "west", "south", "east"],
        "is_fasting_day": False,
        "description": "Harvest festival marking sun's transition into Makara rashi.",
        "special_foods": ["Til Ladoo", "Gajak", "Khichdi", "Puran Poli"],
    },
    {
        "name": "Pongal",
        "name_hindi": "पोंगल",
        "date": date(2025, 1, 15),
        "regions": ["south"],
        "is_fasting_day": False,
        "description": "Tamil harvest festival celebrating the sun god.",
        "special_foods": ["Pongal", "Vadai", "Payasam", "Murukku"],
    },
    {
        "name": "Republic Day",
        "name_hindi": "गणतंत्र दिवस",
        "date": date(2025, 1, 26),
        "regions": ["north", "south", "east", "west"],
        "is_fasting_day": False,
        "description": "National holiday celebrating the constitution.",
        "special_foods": ["Kheer", "Halwa", "Ladoo"],
    },
    {
        "name": "Vasant Panchami",
        "name_hindi": "वसंत पंचमी",
        "date": date(2025, 2, 2),
        "regions": ["north", "east"],
        "is_fasting_day": False,
        "description": "Festival honoring goddess Saraswati and spring.",
        "special_foods": ["Kheer", "Boondi Ladoo", "Yellow Rice"],
    },
    {
        "name": "Maha Shivaratri",
        "name_hindi": "महाशिवरात्रि",
        "date": date(2025, 2, 26),
        "regions": ["north", "south", "east", "west"],
        "is_fasting_day": True,
        "fasting_type": "partial",
        "description": "Night dedicated to Lord Shiva.",
        "special_foods": ["Fruits", "Sabudana Khichdi", "Milk preparations"],
        "avoided_foods": ["Grains", "Rice", "Regular salt"],
    },
    {
        "name": "Holi",
        "name_hindi": "होली",
        "date": date(2025, 3, 14),
        "regions": ["north", "west", "east"],
        "is_fasting_day": False,
        "description": "Festival of colors celebrating spring.",
        "special_foods": ["Gujiya", "Thandai", "Malpua", "Dahi Bhalla"],
    },
    {
        "name": "Ugadi",
        "name_hindi": "उगादी",
        "date": date(2025, 3, 30),
        "regions": ["south"],
        "is_fasting_day": False,
        "description": "Telugu and Kannada New Year.",
        "special_foods": ["Ugadi Pachadi", "Holige", "Pulihora"],
    },
    {
        "name": "Ram Navami",
        "name_hindi": "राम नवमी",
        "date": date(2025, 4, 6),
        "regions": ["north", "south", "east", "west"],
        "is_fasting_day": True,
        "fasting_type": "partial",
        "description": "Birthday of Lord Rama.",
        "special_foods": ["Panakam", "Fruits", "Kheer"],
    },
    {
        "name": "Baisakhi",
        "name_hindi": "बैसाखी",
        "date": date(2025, 4, 14),
        "regions": ["north"],
        "is_fasting_day": False,
        "description": "Punjabi harvest festival and Sikh New Year.",
        "special_foods": ["Kadhi Pakora", "Makki di Roti", "Sarson da Saag"],
    },
    {
        "name": "Raksha Bandhan",
        "name_hindi": "रक्षा बंधन",
        "date": date(2025, 8, 9),
        "regions": ["north", "west"],
        "is_fasting_day": False,
        "description": "Festival celebrating sibling bonds.",
        "special_foods": ["Coconut Barfi", "Ghevar", "Kheer"],
    },
    {
        "name": "Janmashtami",
        "name_hindi": "जन्माष्टमी",
        "date": date(2025, 8, 16),
        "regions": ["north", "west", "south"],
        "is_fasting_day": True,
        "fasting_type": "complete",
        "description": "Birthday of Lord Krishna.",
        "special_foods": ["Panjiri", "Makhan Mishri", "Panchamrit"],
        "avoided_foods": ["Grains", "Regular meals until midnight"],
    },
    {
        "name": "Independence Day",
        "name_hindi": "स्वतंत्रता दिवस",
        "date": date(2025, 8, 15),
        "regions": ["north", "south", "east", "west"],
        "is_fasting_day": False,
        "description": "National independence day.",
        "special_foods": ["Tricolor dishes", "Kheer", "Sweets"],
    },
    {
        "name": "Ganesh Chaturthi",
        "name_hindi": "गणेश चतुर्थी",
        "date": date(2025, 8, 27),
        "regions": ["west", "south"],
        "is_fasting_day": False,
        "description": "Birthday of Lord Ganesha.",
        "special_foods": ["Modak", "Puran Poli", "Ladoo"],
    },
    {
        "name": "Onam",
        "name_hindi": "ओणम",
        "date": date(2025, 8, 29),
        "regions": ["south"],
        "is_fasting_day": False,
        "description": "Kerala's harvest festival.",
        "special_foods": ["Onam Sadya", "Payasam", "Avial", "Olan"],
    },
    {
        "name": "Navratri Begins",
        "name_hindi": "नवरात्रि",
        "date": date(2025, 9, 22),
        "regions": ["north", "west", "south"],
        "is_fasting_day": True,
        "fasting_type": "specific",
        "description": "Nine nights dedicated to goddess Durga.",
        "special_foods": ["Sabudana Khichdi", "Kuttu Paratha", "Fruit Chaat"],
        "avoided_foods": ["Grains", "Onion", "Garlic"],
    },
    {
        "name": "Durga Puja",
        "name_hindi": "दुर्गा पूजा",
        "date": date(2025, 9, 29),
        "regions": ["east"],
        "is_fasting_day": False,
        "description": "Biggest Bengali festival for goddess Durga.",
        "special_foods": ["Luchi", "Alur Dom", "Kosha Mangsho", "Mishti"],
    },
    {
        "name": "Dussehra",
        "name_hindi": "दशहरा",
        "date": date(2025, 10, 2),
        "regions": ["north", "south", "east", "west"],
        "is_fasting_day": False,
        "description": "Victory of good over evil.",
        "special_foods": ["Jalebi", "Fafda", "Kheer"],
    },
    {
        "name": "Karwa Chauth",
        "name_hindi": "करवा चौथ",
        "date": date(2025, 10, 10),
        "regions": ["north"],
        "is_fasting_day": True,
        "fasting_type": "complete",
        "description": "Married women fast for their husbands.",
        "special_foods": ["Sargi", "Mathri", "Poori"],
        "avoided_foods": ["All food and water until moonrise"],
    },
    {
        "name": "Diwali",
        "name_hindi": "दीपावली",
        "date": date(2025, 10, 20),
        "regions": ["north", "south", "east", "west"],
        "is_fasting_day": False,
        "description": "Festival of lights.",
        "special_foods": ["Kaju Katli", "Gulab Jamun", "Samosa", "Chakli"],
    },
    {
        "name": "Govardhan Puja",
        "name_hindi": "गोवर्धन पूजा",
        "date": date(2025, 10, 21),
        "regions": ["north"],
        "is_fasting_day": False,
        "description": "Day after Diwali, worship of Govardhan hill.",
        "special_foods": ["Annakut", "56 dishes"],
    },
    {
        "name": "Bhai Dooj",
        "name_hindi": "भाई दूज",
        "date": date(2025, 10, 22),
        "regions": ["north", "west"],
        "is_fasting_day": False,
        "description": "Sisters pray for brothers' well-being.",
        "special_foods": ["Coconut Ladoo", "Kaju Barfi"],
    },
    {
        "name": "Chhath Puja",
        "name_hindi": "छठ पूजा",
        "date": date(2025, 10, 26),
        "regions": ["east", "north"],
        "is_fasting_day": True,
        "fasting_type": "complete",
        "description": "Sun worship festival from Bihar.",
        "special_foods": ["Thekua", "Kheer", "Fruits"],
        "avoided_foods": ["Onion", "Garlic", "Non-veg"],
    },
    {
        "name": "Christmas",
        "name_hindi": "क्रिसमस",
        "date": date(2025, 12, 25),
        "regions": ["north", "south", "east", "west"],
        "is_fasting_day": False,
        "description": "Celebration of birth of Jesus Christ.",
        "special_foods": ["Plum Cake", "Kulkuls", "Neureos"],
    },
]


async def seed_festivals():
    """Seed the database with festival data."""
    async with async_session_maker() as session:
        # Check if festivals already exist
        result = await session.execute(select(Festival).limit(1))
        if result.scalar_one_or_none():
            print("Festivals already exist. Skipping seed.")
            return

        for festival_data in FESTIVALS:
            festival = Festival(
                id=str(uuid.uuid4()),
                name=festival_data["name"],
                name_hindi=festival_data.get("name_hindi"),
                description=festival_data.get("description"),
                date=festival_data["date"],
                year=festival_data["date"].year,
                regions=festival_data["regions"],
                is_fasting_day=festival_data.get("is_fasting_day", False),
                fasting_type=festival_data.get("fasting_type"),
                special_foods=festival_data.get("special_foods"),
                avoided_foods=festival_data.get("avoided_foods"),
            )
            session.add(festival)

        await session.commit()
        print(f"Seeded {len(FESTIVALS)} festivals successfully!")


if __name__ == "__main__":
    asyncio.run(seed_festivals())

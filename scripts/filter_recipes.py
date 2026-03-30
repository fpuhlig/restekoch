"""
Filter RecipeNLG dataset for quality recipes.

Reads the full 2.2M CSV, applies quality filters, and writes
a clean JSON file that can be used to seed Firestore.

Usage:
    python scripts/filter_recipes.py --input scripts/data/dataset/full_dataset.csv --output scripts/data/recipes.json --limit 2000
"""

import argparse
import csv
import json
import sys


def parse_json_field(raw: str) -> list[str]:
    try:
        return json.loads(raw)
    except (json.JSONDecodeError, TypeError):
        return []


def is_quality_recipe(title: str, ingredients: list[str], directions: list[str]) -> bool:
    if not title or len(title.strip()) < 3:
        return False
    if len(ingredients) < 3:
        return False
    if len(directions) < 2:
        return False
    # skip recipes where any direction step is just one word
    if any(len(step.split()) < 3 for step in directions):
        return False
    return True


def main():
    parser = argparse.ArgumentParser(description="Filter RecipeNLG for quality recipes")
    parser.add_argument("--input", required=True, help="Path to full_dataset.csv")
    parser.add_argument("--output", required=True, help="Output JSON path")
    parser.add_argument("--limit", type=int, default=2000, help="Max recipes to keep")
    args = parser.parse_args()

    recipes = []
    skipped = 0

    with open(args.input, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            title = row.get("title", "").strip()
            ingredients = parse_json_field(row.get("ingredients", "[]"))
            directions = parse_json_field(row.get("directions", "[]"))
            ner = parse_json_field(row.get("NER", "[]"))

            if not is_quality_recipe(title, ingredients, directions):
                skipped += 1
                continue

            recipes.append({
                "title": title,
                "ingredients": ingredients,
                "directions": directions,
                "ner": ner,
            })

            if len(recipes) >= args.limit:
                break

    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(recipes, f, indent=2, ensure_ascii=False)

    print(f"kept {len(recipes)}, skipped {skipped}, written to {args.output}")


if __name__ == "__main__":
    main()

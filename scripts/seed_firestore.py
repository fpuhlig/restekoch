"""
Seed Firestore with recipes from recipes.json.

Uses batch writes (max 400 per batch, Firestore limit is 500).
Deterministic document IDs from title hash for idempotency.

Usage:
    # Against real GCP Firestore (needs gcloud auth application-default login)
    python scripts/seed_firestore.py

    # Against local emulator
    FIRESTORE_EMULATOR_HOST=localhost:8081 python scripts/seed_firestore.py

    # Custom input file or project
    python scripts/seed_firestore.py --input path/to/recipes.json --project my-project
"""

import argparse
import hashlib
import json
import sys
import time

from google.cloud import firestore


BATCH_SIZE = 400
COLLECTION = "recipes"


def make_id(title: str) -> str:
    """Deterministic document ID from title. Same title = same ID = idempotent."""
    return hashlib.sha256(title.encode("utf-8")).hexdigest()[:12]


def seed(db: firestore.Client, recipes: list[dict]) -> int:
    collection = db.collection(COLLECTION)
    total = len(recipes)
    written = 0

    for i in range(0, total, BATCH_SIZE):
        chunk = recipes[i : i + BATCH_SIZE]
        batch = db.batch()

        for recipe in chunk:
            doc_id = make_id(recipe["title"])
            doc_ref = collection.document(doc_id)
            batch.set(doc_ref, recipe)

        batch.commit()
        written += len(chunk)
        batch_num = (i // BATCH_SIZE) + 1
        total_batches = (total + BATCH_SIZE - 1) // BATCH_SIZE
        print(f"  batch {batch_num}/{total_batches}: {written}/{total} recipes")

    return written


def main():
    parser = argparse.ArgumentParser(description="Seed Firestore with recipes")
    parser.add_argument(
        "--input",
        default="scripts/data/recipes.json",
        help="Path to recipes JSON file (default: scripts/data/recipes.json)",
    )
    parser.add_argument(
        "--project",
        default="restekoch",
        help="GCP project ID (default: restekoch)",
    )
    args = parser.parse_args()

    with open(args.input, "r", encoding="utf-8") as f:
        recipes = json.load(f)

    print(f"loaded {len(recipes)} recipes from {args.input}")

    db = firestore.Client(project=args.project)
    start = time.time()
    written = seed(db, recipes)
    elapsed = time.time() - start

    print(f"done: {written} recipes seeded in {elapsed:.1f}s")


if __name__ == "__main__":
    main()

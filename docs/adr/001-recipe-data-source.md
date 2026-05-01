# ADR 001: Recipe Data Source

## Status

Accepted

## Context

We need a recipe database for the RAG pipeline. Looked at three options:

TheMealDB has a free JSON API with clean data and images, but only 598 recipes. That is too few for vector search to produce useful results.

Spoonacular has 380k+ recipes and a good API, but the free tier caps at 150 calls/day. Adds a runtime dependency we do not control.

RecipeNLG is a 2.2M recipe dataset from a research project. Free download from HuggingFace as CSV. Each entry has a title, a parsed ingredient list, and directions. No images.

## Decision

RecipeNLG. We download it once, filter for quality entries, and seed about 500-1000 recipes into Firestore.

## Why

The main reason is no runtime dependency. Recipes live in our own database. We pick what goes in and control the quality.

The dataset has parsed ingredient lists with named entity recognition already done. That saves us work when generating embeddings later.

500-1000 recipes is enough for the demo. More would not improve the presentation but would slow down seeding and cost more on Vertex AI embeddings.

## Trade-offs

No images. The UI will be text-only for recipes. Acceptable for this project.

Data quality is inconsistent. Some entries have missing fields or nonsensical ingredients. The seed script needs to filter strictly: skip anything with fewer than 3 ingredients or missing directions.

package com.restekoch.recipe

import com.google.cloud.firestore.Firestore
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class RecipeRepository(
    val firestore: Firestore,
) {
    private val collection get() = firestore.collection("recipes")

    fun findAll(
        limit: Int = 20,
        offset: Int = 0,
    ): List<Recipe> {
        return collection.offset(offset).limit(limit).get().get().documents.map { doc ->
            doc.toObject(Recipe::class.java).copy(id = doc.id)
        }
    }

    fun findById(id: String): Recipe? {
        val doc = collection.document(id).get().get()
        if (!doc.exists()) return null
        return doc.toObject(Recipe::class.java)?.copy(id = doc.id)
    }

    fun save(recipe: Recipe): String {
        val docRef =
            if (recipe.id.isNotBlank()) {
                collection.document(recipe.id)
            } else {
                collection.document()
            }
        docRef.set(recipe).get()
        return docRef.id
    }

    fun saveAll(recipes: List<Recipe>) {
        recipes.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { recipe ->
                val docRef = collection.document()
                batch.set(docRef, recipe)
            }
            batch.commit().get()
        }
    }
}

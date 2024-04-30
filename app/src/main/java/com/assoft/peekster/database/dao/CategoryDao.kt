package com.assoft.peekster.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.assoft.peekster.database.entities.Category

/**
 * The Data Access Object for the [Category] class.
 */
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAll(): LiveData<List<Category>>

    @Insert
    fun insertAll(vararg categories: Category)

    @Insert
    fun insert(category: Category)
    
    @Update
    fun update(category: Category)
    
    @Delete
    fun deleteCategory(category: Category)

    @Query("DELETE FROM categories")
    fun deleteAllCategories()
}
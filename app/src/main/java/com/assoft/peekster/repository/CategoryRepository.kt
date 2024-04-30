package com.assoft.peekster.repository

import android.os.AsyncTask
import androidx.lifecycle.LiveData
import com.assoft.peekster.database.dao.CategoryDao
import com.assoft.peekster.database.entities.Category

class CategoryRepository(private val categoryDao: CategoryDao) {

    private val allCategories: LiveData<List<Category>> = categoryDao.getAll()

    fun insert(category: Category) {
        InsertCategoryAsyncTask(
            categoryDao
        ).execute(category)
    }

    fun update(category: Category) {
        UpdateCategoryCountAsyncTask(
            categoryDao
        ).execute(category)
    }

    fun delete(category: Category) {
        DeleteCategoryAsyncTask(
            categoryDao
        ).execute(category)
    }

    fun deleteAllCategories() {
        DeleteAllCategoriesAsyncTask(
            categoryDao
        ).execute()
    }

    fun getAllCategories(): LiveData<List<Category>> {
        return allCategories
    }

    private class DeleteCategoryAsyncTask(val categoryDao: CategoryDao) :
        AsyncTask<Category, Unit, Unit>() {
        override fun doInBackground(vararg category: Category) {
            categoryDao.deleteCategory(category = category[0])
        }
    }

    private class UpdateCategoryCountAsyncTask(val categoryDao: CategoryDao) :
        AsyncTask<Category, Unit, Unit>() {
        override fun doInBackground(vararg category: Category) {
            categoryDao.update(category[0])
            return
        }
    }

    private class InsertCategoryAsyncTask(val categoryDao: CategoryDao) :
        AsyncTask<Category, Unit, Unit>() {

        override fun doInBackground(vararg category: Category) {
            categoryDao.insert(category = category[0])
        }
    }

    private class DeleteAllCategoriesAsyncTask(val categoryDao: CategoryDao) :
        AsyncTask<Unit, Unit, Unit>() {

        override fun doInBackground(vararg p0: Unit?) {
            categoryDao.deleteAllCategories()
            return
        }
    }
}

data class UploadCategoryParam(
    val count: Int,
    val name: String
)
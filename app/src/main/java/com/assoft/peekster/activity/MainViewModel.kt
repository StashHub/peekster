/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.assoft.peekster.activity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.assoft.peekster.domain.HttpServer
import com.assoft.peekster.domain.usecase.*
import com.assoft.peekster.repository.ServerRepository

class MainViewModel(
    private val introductionShownActionUseCase: IntroductionShownActionUseCase,
    val introductionShownUseCase: IntroductionShownUseCase,
    private val getDeviceNameUseCase: GetDeviceNameUseCase,
    private val saveDeviceNameActionUseCase: SaveDeviceNameActionUseCase,
    private val serverRepository: ServerRepository
) : ViewModel(), ServerListener {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _introductionShown = MutableLiveData<Boolean>()
    val introductionShown: LiveData<Boolean> get() = _introductionShown

    private var server: LiveData<HttpServer?>? = serverRepository.getServer()

    init {
        val introductionShownResult = introductionShownUseCase()
        _introductionShown.postValue(introductionShownResult)
    }

    fun introductionShown() = introductionShownUseCase()

    fun getDeviceName(): String? = getDeviceNameUseCase()

    fun getServer(): LiveData<HttpServer?>? {
        return server
    }

    fun getStarted() {
        _loading.value = true
        introductionShownActionUseCase(true)
        _introductionShown.value = true
    }

    fun stopLoading(isRunning: Boolean) {
        _loading.value = isRunning
    }

    fun saveDeviceName(name: String) {
        saveDeviceNameActionUseCase(name)
    }

    override fun insertServer(port: Int, data: String, url: String) {
        serverRepository.insert(
            HttpServer(
                port = port,
                data = data,
                url = url
            )
        )
    }

    override fun delete(port: Int) {
        serverRepository.delete(port)
    }
}

interface ServerListener {
    fun insertServer(
        port: Int,
        data: String,
        url: String
    )

    fun delete(port: Int)
}
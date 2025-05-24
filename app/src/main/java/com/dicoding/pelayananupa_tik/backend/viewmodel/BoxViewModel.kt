package com.dicoding.pelayananupa_tik.backend.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dicoding.pelayananupa_tik.backend.model.Barang

class BoxViewModel : ViewModel() {
    private val _boxItems = MutableLiveData<MutableList<Barang>>(mutableListOf())
    val boxItems: LiveData<MutableList<Barang>> = _boxItems

    private val _boxCount = MutableLiveData(0)
    val boxCount: LiveData<Int> = _boxCount

    // For checkbox selections
    private val _selectedItems = MutableLiveData<MutableList<Barang>>(mutableListOf())
    val selectedItems: LiveData<MutableList<Barang>> = _selectedItems

    fun addToBox(barang: Barang) {
        val currentList = _boxItems.value ?: mutableListOf()
        val existingItem = currentList.find { it.namaBarang == barang.namaBarang }
        if (existingItem == null) {
            currentList.add(barang)
            _boxItems.value = currentList
            _boxCount.value = currentList.size
        }
    }

    fun removeFromBox(barang: Barang) {
        val currentList = _boxItems.value ?: mutableListOf()
        currentList.removeAll { it.namaBarang == barang.namaBarang }
        _boxItems.value = currentList
        _boxCount.value = currentList.size
    }

    fun clearBox() {
        _boxItems.value = mutableListOf()
        _boxCount.value = 0
    }

    fun getBoxCount(): Int = _boxItems.value?.size ?: 0

    fun isItemInBox(barang: Barang): Boolean {
        return _boxItems.value?.any { it.namaBarang == barang.namaBarang } ?: false
    }

    // Selection methods
    fun selectItem(barang: Barang) {
        val currentSelected = _selectedItems.value ?: mutableListOf()
        if (!currentSelected.any { it.namaBarang == barang.namaBarang }) {
            currentSelected.add(barang)
            _selectedItems.value = currentSelected
        }
    }

    fun unselectItem(barang: Barang) {
        val currentSelected = _selectedItems.value ?: mutableListOf()
        currentSelected.removeAll { it.namaBarang == barang.namaBarang }
        _selectedItems.value = currentSelected
    }

    fun isItemSelected(barang: Barang): Boolean {
        return _selectedItems.value?.any { it.namaBarang == barang.namaBarang } ?: false
    }

    fun selectAll() {
        _selectedItems.value = _boxItems.value?.toMutableList() ?: mutableListOf()
    }

    fun unselectAll() {
        _selectedItems.value = mutableListOf()
    }

    fun getSelectedCount(): Int = _selectedItems.value?.size ?: 0

    fun getSelectedItems(): List<Barang> = _selectedItems.value ?: emptyList()

    fun clearAllItems() {
        _selectedItems.value = mutableListOf()
    }
}
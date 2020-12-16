package com.platzi.android.rickandmorty.usecases

import com.platzi.android.rickandmorty.database.CharacterDao
import io.reactivex.schedulers.Schedulers

class GetAllFavoriteCharactersUseCase (
    private val characterDao: CharacterDao
) {

    fun invoke() = characterDao
        .getAllFavoriteCharacters()
        .onErrorReturn { emptyList() }
        .subscribeOn(Schedulers.io())

}
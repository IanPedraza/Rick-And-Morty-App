package com.platzi.android.rickandmorty.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.platzi.android.rickandmorty.R
import com.platzi.android.rickandmorty.adapters.EpisodeListAdapter
import com.platzi.android.rickandmorty.api.APIConstants.BASE_API_URL
import com.platzi.android.rickandmorty.api.CharacterServer
import com.platzi.android.rickandmorty.api.EpisodeRequest
import com.platzi.android.rickandmorty.api.EpisodeService
import com.platzi.android.rickandmorty.api.toCharacterEntity
import com.platzi.android.rickandmorty.database.CharacterDao
import com.platzi.android.rickandmorty.database.CharacterDatabase
import com.platzi.android.rickandmorty.database.CharacterEntity
import com.platzi.android.rickandmorty.databinding.ActivityCharacterDetailBinding
import com.platzi.android.rickandmorty.presentation.CharacterDetailViewModel
import com.platzi.android.rickandmorty.presentation.CharacterDetailViewModel.CharacterDetailNavigation
import com.platzi.android.rickandmorty.presentation.CharacterDetailViewModel.CharacterDetailNavigation.*
import com.platzi.android.rickandmorty.presentation.Event
import com.platzi.android.rickandmorty.usecases.GetEpisodesFromCharacterUseCase
import com.platzi.android.rickandmorty.usecases.GetFavoriteCharacterStatusUseCase
import com.platzi.android.rickandmorty.usecases.UpdateFavoriteCharacterStatusUseCase
import com.platzi.android.rickandmorty.utils.Constants
import com.platzi.android.rickandmorty.utils.bindCircularImageUrl
import com.platzi.android.rickandmorty.utils.getViewModel
import com.platzi.android.rickandmorty.utils.showLongToast
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_character_detail.*

class CharacterDetailActivity : AppCompatActivity() {

    //region Fields

    private lateinit var episodeListAdapter: EpisodeListAdapter
    private lateinit var binding: ActivityCharacterDetailBinding

    private val episodeRequest: EpisodeRequest by lazy {
        EpisodeRequest(BASE_API_URL)
    }

    private val characterDao: CharacterDao by lazy {
        CharacterDatabase.getDatabase(application).characterDao()
    }

    private val getEpisodesFromCharacterUseCase: GetEpisodesFromCharacterUseCase by lazy {
        GetEpisodesFromCharacterUseCase(episodeRequest)
    }

    private val getFavoriteCharacterStatusUseCase by lazy {
        GetFavoriteCharacterStatusUseCase(characterDao)
    }

    private val updateFavoriteCharacterStatusUseCase by lazy {
        UpdateFavoriteCharacterStatusUseCase(characterDao)
    }

    private val characterDetailViewModel: CharacterDetailViewModel by lazy {
        getViewModel {
            CharacterDetailViewModel(
                intent.getParcelableExtra(Constants.EXTRA_CHARACTER),
                getFavoriteCharacterStatusUseCase,
                updateFavoriteCharacterStatusUseCase,
                getEpisodesFromCharacterUseCase
            )
        }
    }

    //endregion

    //region Override Methods

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_character_detail)
        binding.lifecycleOwner = this@CharacterDetailActivity

        episodeListAdapter = EpisodeListAdapter { episode ->
            this@CharacterDetailActivity.showLongToast("Episode -> $episode")
        }
        rvEpisodeList.adapter = episodeListAdapter

        characterFavorite.setOnClickListener { characterDetailViewModel.onUpdateFavoriteCharacterStatus() }

        characterDetailViewModel.characterValues.observe(this, Observer(this::loadCharacter))
        characterDetailViewModel.isFavorite.observe(this, Observer(this::updateFavoriteIcon))
        characterDetailViewModel.events.observe(this, Observer(this::validateEvents))

        characterDetailViewModel.onCharacterValidation()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    //endregion

    //region Private Methods

    private fun loadCharacter(character: CharacterServer) {
        binding.characterImage.bindCircularImageUrl(
            url = character.image,
            placeholder = R.drawable.ic_camera_alt_black,
            errorPlaceholder = R.drawable.ic_broken_image_black
        )
        binding.characterDataName = character.name
        binding.characterDataStatus = character.status
        binding.characterDataSpecies = character.species
        binding.characterDataGender = character.gender
        binding.characterDataOriginName = character.origin.name
        binding.characterDataLocationName = character.location.name

    }

    private fun updateFavoriteIcon(isFavorite: Boolean?) {
        characterFavorite.setImageResource(
            if (isFavorite != null && isFavorite) {
                R.drawable.ic_favorite
            } else {
                R.drawable.ic_favorite_border
            }
        )
    }

    private fun validateEvents(event: Event<CharacterDetailNavigation>?) {
        event?.getContentIfNotHandled()?.let { navigation ->
            when (navigation) {
                is ShowEpisodeError -> navigation.run {
                    this@CharacterDetailActivity.showLongToast("Error -> ${error.message}")
                }
                is ShowEpisodeList -> navigation.run {
                    episodeListAdapter.updateData(episodeList)
                }
                CloseActivity -> {
                    this@CharacterDetailActivity.showLongToast(R.string.error_no_character_data)
                    finish()
                }
                HideEpisodeListLoading -> {
                    episodeProgressBar.isVisible = false
                }
                ShowEpisodeListLoading -> {
                    episodeProgressBar.isVisible = true
                }
            }
        }
    }

    //endregion
}
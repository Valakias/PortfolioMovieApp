package com.degiorgi.valerio.portfoliomovieapp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.degiorgi.valerio.portfoliomovieapp.data.MovieContentProvider;
import com.degiorgi.valerio.portfoliomovieapp.data.MovieDatabaseContract;
import com.degiorgi.valerio.portfoliomovieapp.models.MovieApiRequest;
import com.degiorgi.valerio.portfoliomovieapp.models.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * Created by Valerio on 23/01/2016.
 */
public class MovieFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String API_BASE_URL = "http://api.themoviedb.org/";
    public final static int LOADER_ID = 1;
    public CursorMovieAdapter mCursorAdapater;
    List<Result> Movies = new ArrayList<>();
    Call<MovieApiRequest> CallMovies;
    String api_key = "241141bc665e9b2d0fb9ac4759497786";

    private void UpdateMovies() { //Executes the background Network Call

        Retrofit retrofit = new Retrofit.Builder().baseUrl(API_BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();

        MovieService.FetchMovieInterface MovieInterface = retrofit.create(MovieService.FetchMovieInterface.class);


        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String sortBy = preferences.getString(getString(R.string.sort_by_key), getString(R.string.sort_by_default));

        CallMovies = MovieInterface.getMovies(sortBy, api_key);

        CallMovies.enqueue(new Callback<MovieApiRequest>() {

            @Override
            public void onResponse(Call<MovieApiRequest> call, Response<MovieApiRequest> response) {

                if (response != null) {

                    MovieApiRequest request = response.body();
                    Movies = request.getResults();

                    ContentResolver resolver = getActivity().getContentResolver();
                    String[] args = {Movies.get(0).getId().toString()};

                    Cursor cur = resolver.query(MovieContentProvider.Local_Movies.CONTENT_URI, null, MovieDatabaseContract.MovieId + "=?",
                            args, null);

                    if (cur.moveToFirst()) {
                    } else {
                        Vector<ContentValues> cVector = new Vector<ContentValues>(Movies.size());

                        for (int i = 0; i < Movies.size(); i++) {


                            ContentValues MovieValues = new ContentValues();

                            int id = Movies.get(i).getId();
                            String posterPath = Movies.get(i).getPosterPath();
                            String overview = Movies.get(i).getOverview();
                            String releaseDate = Movies.get(i).getReleaseDate();
                            String originalTitle = Movies.get(i).getOriginalTitle();
                            double voteAverage = Movies.get(i).getVoteAverage();
                            double popularity = Movies.get(i).getPopularity();


                            MovieValues.put(MovieDatabaseContract.MovieId, id);
                            MovieValues.put(MovieDatabaseContract.PosterUrl, posterPath);
                            MovieValues.put(MovieDatabaseContract.OriginalTitle, originalTitle);
                            MovieValues.put(MovieDatabaseContract.Overview, overview);
                            MovieValues.put(MovieDatabaseContract.ReleaseDate, releaseDate);
                            MovieValues.put(MovieDatabaseContract.UserRating, voteAverage);
                            MovieValues.put(MovieDatabaseContract.Popularity, popularity);


                            cVector.add(MovieValues);
                        }

                        int inserted = 0;
                        if (cVector.size() > 0) {

                            ContentValues[] cArray = new ContentValues[cVector.size()];
                            cVector.toArray(cArray);
                            inserted = getActivity().getContentResolver().bulkInsert(MovieContentProvider.Local_Movies.CONTENT_URI, cArray);


                        }

                        Log.w("LOG TAG", "INSERTED" + inserted);
                        ;

                    }
                }
            }

            @Override
            public void onFailure(Call<MovieApiRequest> call, Throwable t) {

            }
        });

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }


    @Override

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

// Create a gridview reference, initialize the adapater, and assign it to the gridview

        mCursorAdapater = new CursorMovieAdapter(getActivity(), null, 0);

        UpdateMovies();

        View rootView = inflater.inflate(R.layout.movie_fragment_layout, container, false);

        GridView gridview = (GridView) rootView.findViewById(R.id.gridview_list);

        gridview.setAdapter(mCursorAdapater);


        return rootView;

    }

    @Override
    public void onStop() {
        super.onStop();
        CallMovies.cancel();
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String sortBy = preferences.getString(getString(R.string.sort_by_key), getString(R.string.sort_by_default));

        String sortOrder;
        if(sortBy != getString(R.string.sort_by_popularity_value))
        {
            sortOrder = MovieDatabaseContract.UserRating +"  DESC";
        }
        else {

            sortOrder = MovieDatabaseContract.Popularity + " DESC";

        }


        return new CursorLoader(getActivity(), MovieContentProvider.Local_Movies.CONTENT_URI,
                null, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapater.swapCursor(data);
    }


    @Override
    public void onLoaderReset(Loader loader) {

        mCursorAdapater.swapCursor(null);
    }


}




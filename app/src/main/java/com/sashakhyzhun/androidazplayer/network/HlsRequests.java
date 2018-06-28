package com.sashakhyzhun.androidazplayer.network;

import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface HlsRequests {

    @HEAD("{file}")
    Single<Request> headRx(
            @Path("file") String file
    );

    @HEAD("{file}")
    Call<Void> headCall(
            @Path("file") String file
    );

    @GET("{file_name}")
    Observable<ResponseBody> downloadChunk(
            @Path("file_name") String fileName,
            @Query("Range") String range
    );


}

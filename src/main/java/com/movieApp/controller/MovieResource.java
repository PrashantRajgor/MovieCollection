package com.movieApp.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.movieApp.entity.Movie;
import com.movieApp.entity.MovieData;

@RestController
public class MovieResource {

	private final Logger logger = LoggerFactory.getLogger(MovieResource.class);
	
	@Autowired
	private RestTemplate restTemplate;

	private static String url = "https://jsonmock.hackerrank.com/api/movies/search/";
	private static List<Movie> threadList = new CopyOnWriteArrayList<Movie>();
	private static List<String> titles = new ArrayList<String>();

	@GetMapping("movie")
	public List<String> getMovieTitles(@RequestParam("name") String name)
			throws InterruptedException, ExecutionException {
		logger.info("Executing getMovieTitles, movie name : {}",name);
		ExecutorService executorService = Executors.newFixedThreadPool(10);
		MovieResponse movieResponse = new MovieResponse(name, 1, restTemplate);
		MovieData tempMovie = movieResponse.getResponse();
		int temp = tempMovie.getTotal_pages();
		threadList.addAll(tempMovie.getData());
		
		for (int i = 2; i <= temp; i++) {

			Runnable requestThread = new MovieResponse(name, i, restTemplate);
			executorService.execute(requestThread);

		}
		executorService.shutdown();
		while (!executorService.isTerminated()) {

		}

		threadList.forEach(x -> titles.add(x.getTitle()));
		Collections.sort(titles);
		logger.info("Executed getMovieTitles ");
		return titles;

	}

	static class MovieResponse implements Runnable {

		String word;
		int page;
		RestTemplate template;

		public MovieResponse(String word, int page, RestTemplate template) {
			this.word = word;
			this.page = page;
			this.template = template;
		}

		@Override
		public void run() {
			threadList.addAll(getResponse().getData());
		}
		
		public MovieData getResponse(){

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			HttpEntity<String> requestEntity = new HttpEntity<String>(headers);

			return template.exchange(url + "?Title=" + word + "&page=" + page, HttpMethod.GET, requestEntity,
					MovieData.class).getBody();
			
		}
	}

}

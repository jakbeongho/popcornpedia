package com.popcornpedia.admin.controllor;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.popcornpedia.member.dto.MemberDTO;
import com.popcornpedia.movie.dto.MovieDTO;

public interface AdminMovieController {
	
	public ModelAndView selectAllMovie(@RequestParam(defaultValue = "1") int num) throws Exception;
	public ModelAndView MovieForm(HttpServletRequest request, HttpServletResponse response) throws Exception;
	public ModelAndView insertMovie(@ModelAttribute("movieInfo") MovieDTO movieDTO, HttpServletRequest request, HttpServletResponse response)
			throws Exception;
	public ModelAndView updateMovieForm(@RequestParam("movie_id") String movie_id, HttpServletRequest request, HttpServletResponse response)
			throws Exception;
	public String updateMovie(MovieDTO movieDTO, HttpServletRequest request, HttpServletResponse response) throws Exception;
	public ModelAndView deleteMovie(@RequestParam("movie_id") String movie_id, HttpServletRequest request, HttpServletResponse response)
			throws Exception;
	public ModelAndView goMovieInfoByID(@RequestParam("movie_id") String movie_id, HttpServletRequest request, HttpServletResponse response, Model model) throws Exception;
	public ModelAndView goMovieInfoByCD(@RequestParam("movieCd") String movieCd, HttpServletRequest request, HttpServletResponse response, Model model) throws Exception;
	public ModelAndView goSearchForm(HttpServletRequest request, HttpServletResponse response) throws Exception;
	public ModelAndView adminSearchMovie(@RequestParam HashMap<String, Object> searchMap, @RequestParam(defaultValue = "1") int num, HttpServletRequest request, HttpServletResponse response) throws Exception;
	public ModelAndView userMovieSearch(@RequestParam HashMap<String, Object> searchMap, @RequestParam(defaultValue = "1") int num, @RequestParam("keyword") String keyword, HttpServletRequest request, HttpServletResponse response, Model model) throws Exception;
	public void wishMovie(int movie_id, String member_id, HttpServletResponse response) throws Exception;
}

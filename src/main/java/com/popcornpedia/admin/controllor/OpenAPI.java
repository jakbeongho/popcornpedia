package com.popcornpedia.admin.controllor;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcornpedia.admin.service.AdminMovieService;
import com.popcornpedia.movie.dto.MovieDTO;

import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.TmdbMovies;
import info.movito.themoviedbapi.model.MovieDb;
import kr.or.kobis.kobisopenapi.consumer.rest.KobisOpenAPIRestService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Controller("OpenAPIController")
public class OpenAPI {
	
	@Autowired
	private AdminMovieService movieService;
	
	String key = "aacda2bad5836d6156ba855b1db4461a"; //Kobis(영진위) 발급키 
	KobisOpenAPIRestService service = new KobisOpenAPIRestService(key);
	
	//메인 화면
	@RequestMapping( value =  {"/", "/movie/mainMovie"})
	public ModelAndView getBoxOfficeKobis(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		//어제 날짜 구하는 메서드 (일간 박스오피스)
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date yesterday = cal.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String date = dateFormat.format(yesterday);
        //System.out.println("어제 날짜 (yyyyMMdd 형식): " + formattedYesterday);
		
        /*
		//지난주 일요일 날짜 구하는 메서드 (주간 박스오피스)
		cal.add(Calendar.DATE, -7);
		int nWeek = cal.get(Calendar.DAY_OF_WEEK);
		cal.add(Calendar.DATE, 2-nWeek);//요일에서 월요일 되도록 뺌	
		//지난주 일요일
		cal.add(Calendar.DATE, 6);//월요일 부터 일요일까지의 날짜 더함
		int nMonth  = cal.get(Calendar.MONTH)+1;
		String dayLastWeekTo = cal.get(Calendar.YEAR) + (nMonth<10?"0"+nMonth:nMonth+"") + (cal.get(Calendar.DATE)<10?"0"+cal.get(Calendar.DATE):cal.get(Calendar.DATE)+"");
		System.out.println("dayLastWeekTo:"+dayLastWeekTo);
		*/
		
		//박스오피스 api 변수설정
		String targetDt = date;
		String itemPerPage =request.getParameter("itemPerPage");
		String multiMovieYn = request.getParameter("multiMovieYn");
		String repNationCd = request.getParameter("repNationCd");
		String wideAreaCd = request.getParameter("wideAreaCd");
		
		//일일 박스오피스
		String dailyResponse = service.getDailyBoxOffice(true, targetDt, itemPerPage, multiMovieYn, repNationCd, wideAreaCd);
		ObjectMapper mapper = new ObjectMapper();	
		HashMap<String, Object> dailyResult = mapper.readValue(dailyResponse, HashMap.class); //가져온 json정보를 hashmap에 담는다.
		request.setAttribute("dailyResult", dailyResult);
		
		//박스오피스 포스터 이미지 담기(이미지 없으면 준비중.png)
		
		JSONObject obj = new JSONObject(dailyResult);
		List<Map<String,String>> posterElementList = new ArrayList<>();
		for(int i=0; i<10; i++) {
			
			Map<String,String> posterElement = new HashMap<>();
			
			String movieNm = obj.getJSONObject("boxOfficeResult").getJSONArray("dailyBoxOfficeList").getJSONObject(i).getString("movieNm");
			movieNm = normalize(movieNm);
			
			posterElement.put("movieNm",movieNm);
			posterElement.put("movieYear",obj.getJSONObject("boxOfficeResult").getJSONArray("dailyBoxOfficeList").getJSONObject(i).getString("openDt").substring(0,4));
			posterElementList.add(posterElement);
			
		}
		
		//누적 관객수를 만 단위로 표시하고 JSON에 추가하기
		HashMap<String, Object> boxOfficeResult = (HashMap<String, Object>) dailyResult.get("boxOfficeResult");
		ArrayList<HashMap<String, Object>> dailyBoxOfficeList = (ArrayList<HashMap<String, Object>>) boxOfficeResult.get("dailyBoxOfficeList");
		//List<String> audiList = new ArrayList<>();
		for(int i=0; i<10; i++) {
			int audi = Integer.parseInt(obj.getJSONObject("boxOfficeResult").getJSONArray("dailyBoxOfficeList").getJSONObject(i).getString("audiAcc"));
			HashMap<String, Object> movie = (HashMap<String, Object>) dailyBoxOfficeList.get(i);
			String audien = null;
			if(audi > 10000) {
				audien = (audi/10000)+"만명";
			}
			else {
				DecimalFormat df = new DecimalFormat("###,###");
				audien = df.format(audi)+"명";
			}
			movie.put("audi", audien);
		}
		
		for(int i=0; i<10; i++) {
			int rankInten = Integer.parseInt(obj.getJSONObject("boxOfficeResult").getJSONArray("dailyBoxOfficeList").getJSONObject(i).getString("rankInten"));
			HashMap<String, Object> movie = (HashMap<String, Object>) dailyBoxOfficeList.get(i);
			String dropRank = null;
			String sameRank=null;
			if(rankInten<0) {
				dropRank = String.valueOf(rankInten*-1);
			}
			movie.put("drop", dropRank);
		}
		
		
		
		SimpleDateFormat dateformat2 = new SimpleDateFormat("yyyy-MM-dd");
		String date2 = dateformat2.format(yesterday);
		request.setAttribute("date", date2);
		request.setAttribute("posterElementList",posterElementList);

		
		/*		
		//주간 박스오피스
		//날짜를 월요일로 설정해야 하는듯
		String weekResponse = service.getWeeklyBoxOffice(true, dayLastWeekTo, "10", "0", multiMovieYn, repNationCd, wideAreaCd);
		HashMap<String, Object> weekResult = mapper.readValue(weekResponse, HashMap.class);
		request.setAttribute("weekResult", weekResult);
		*/
		
		Map<String, List> collection = movieService.getCollection();
		ModelAndView mav = new ModelAndView("/movie/mainmovie");
		mav.addObject("collection", collection);
		if(boxOfficeResult.isEmpty()) {
			mav.setViewName("/board/");
		}
		return mav;
	}
	

	//영화 입력,수정 FORM에 TMDB에서 오픈 API 정보 가져오기 (관리자 - 영화 조회 - 수동 추가할 때 사용1....)
	@RequestMapping(value = "/admin/getposterurl")
	public void findPosterPath(@RequestParam("movieID") int movieID, HttpServletRequest request, HttpServletResponse response) throws IOException{
		System.out.println("getPostURL 가져온 값 : " + movieID);
		request.setCharacterEncoding("utf-8");
		response.setContentType("text/html; charset=utf-8");
		TmdbMovies movies = new TmdbApi("bca7c5aae23ca2a083040f3f174f17a2").getMovies();
		MovieDb movie = movies.getMovie(movieID, "ko-kr");
		String posterPath = movie.getPosterPath();
		String overView = movie.getOverview();
		String backdropPath = movie.getBackdropPath();
		
		//담기
		PrintWriter pw = response.getWriter();
		JSONObject jo = new JSONObject();
		jo.put("url", posterPath);
		jo.put("overview", overView);
		jo.put("backurl", backdropPath);
		System.out.println("----------getPosterURL : " +posterPath + " 재확인 jo.getString(url) : " +jo.getString("url"));
		pw.print(jo);
	}
	
	
	//영화 입력,수정 FORM에 영진위 정보 가져오기 (관리자 - 영화 조회 - 수동 추가할 때 사용2....)
	@RequestMapping(value = "/admin/getkobisAPI")
	public void getKobisAPI(HttpServletRequest request, HttpServletResponse response) throws Exception{
		
		request.setCharacterEncoding("utf-8");
		response.setContentType("text/html; charset=utf-8");
		
		String curPage = request.getParameter("curPage")==null?"1":request.getParameter("curPage");					//현재페이지
		String itemPerPage = request.getParameter("itemPerPage")==null?"30":request.getParameter("itemPerPage");		//결과row수
		String movieNm = request.getParameter("movieNm")==null?"":request.getParameter("movieNm");						//영화명
		String directorNm = request.getParameter("directorNm")==null?"":request.getParameter("directorNm");				//감독명
		String openStartDt = request.getParameter("openStartDt")==null?"":request.getParameter("openStartDt");			//개봉연도 시작조건 ( YYYY )
		String openEndDt = request.getParameter("openEndDt")==null?"":request.getParameter("openEndDt");				//개봉연도 끝조건 ( YYYY )	
		String prdtStartYear = request.getParameter("prdtStartYear")==null?"":request.getParameter("prdtStartYear");	//제작연도 시작조건 ( YYYY )
		String prdtEndYear = request.getParameter("prdtEndYear")==null?"":request.getParameter("prdtEndYear");			//제작연도 끝조건    ( YYYY )
		String repNationCd = request.getParameter("repNationCd")==null?"":request.getParameter("repNationCd");			//대표국적코드 (공통코드서비스에서 '2204'로 조회된 국가코드)
		String[] movieTypeCdArr = request.getParameterValues("movieTypeCdArr")==null? null:request.getParameterValues("movieTypeCdArr");	//영화형태코드 배열 (공통코드서비스에서 '2201'로 조회된 영화형태코드)
		
		// 영화코드조회 서비스 호출 (boolean isJson, String curPage, String itemPerPage,String directorNm, String movieCd, String movieNm, String openStartDt,String openEndDt, String ordering, String prdtEndYear, String prdtStartYear, String repNationCd, String[] movieTypeCdArr)
	    String movieCdResponse = service.getMovieList(true, curPage, itemPerPage, movieNm, directorNm, openStartDt, openEndDt, prdtStartYear, prdtEndYear, repNationCd, movieTypeCdArr);
		
		// Json 라이브러리를 통해 Handling
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String,Object> result = mapper.readValue(movieCdResponse, HashMap.class);
		
		JSONObject obj = new JSONObject(result);
		String movieCd = obj.getJSONObject("movieListResult").getJSONArray("movieList").getJSONObject(0).getString("movieCd");
		String movie_id = obj.getJSONObject("movieListResult").getJSONArray("movieList").getJSONObject(0).getString("movieNm");
		System.out.println("영화이름 : " + movie_id + "----- 영화코드: " + movieCd);
		
		System.out.println("영진위API 매핑 확인 제목 : " + movieNm + ",  감독 :" + directorNm);
		
		String movieInfo = service.getMovieInfo(true, movieCd);
		
		JSONObject obj2 = new JSONObject(movieInfo);
		String actors = obj2.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getJSONArray("actors").getJSONObject(0).getString("peopleNm");
		for(int i=1; i<obj2.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getJSONArray("actors").length(); i++) {
			actors += ", " + obj2.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getJSONArray("actors").getJSONObject(i).getString("peopleNm");
		}
		String showTm =  obj2.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getString("showTm");
		String grade = obj2.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getJSONArray("audits").getJSONObject(0).getString("watchGradeNm");
		String movieNmEn = obj2.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getString("movieNmEn");
		
		String movieCd2 = obj.getJSONObject("movieListResult").getJSONArray("movieList").getJSONObject(0).getString("movieCd");
		
		PrintWriter pw = response.getWriter();
		JSONObject jo = new JSONObject();
		jo.put("movieCd2", movieCd2);
		jo.put("actors", actors);
		jo.put("showTm", showTm);
		jo.put("grade",grade);
		jo.put("movieNmEn", movieNmEn);
		pw.print(jo);
		System.out.println("배우진 : " + actors);
	}
	
	
	//Kobis (영진위) 검색결과   (포스터까지 모두 출력 >>속도 느림<<)
	@RequestMapping(value = "/movie/searchResultKobis.do")
	public ModelAndView searchMovieKobis(@ModelAttribute("keyword") MovieDTO movieDTO, HttpServletRequest request, HttpServletResponse response, Model model) throws Exception{	
		
		request.setCharacterEncoding("utf-8");
		System.out.println("영진위 검색결과 키워드 : " + movieDTO.getKeyword());
		
		// 파라메터 설정
		String curPage = request.getParameter("curPage")==null?"1":request.getParameter("curPage");					//현재페이지
		String itemPerPage = request.getParameter("itemPerPage")==null?"30":request.getParameter("itemPerPage");		//결과row수
		String movieNm = movieDTO.getKeyword()==null?"":movieDTO.getKeyword();						//영화명
		String directorNm = request.getParameter("directorNm")==null?"":request.getParameter("directorNm");				//감독명
		String openStartDt = request.getParameter("openStartDt")==null?"1950":request.getParameter("openStartDt");			//개봉연도 시작조건 ( YYYY )
		String openEndDt = request.getParameter("openEndDt")==null?"":request.getParameter("openEndDt");				//개봉연도 끝조건 ( YYYY )	
		String prdtStartYear = request.getParameter("prdtStartYear")==null?"1950":request.getParameter("prdtStartYear");	//제작연도 시작조건 ( YYYY )
		String prdtEndYear = request.getParameter("prdtEndYear")==null?"":request.getParameter("prdtEndYear");			//제작연도 끝조건    ( YYYY )
		String repNationCd = request.getParameter("repNationCd")==null?"":request.getParameter("repNationCd");			//대표국적코드 (공통코드서비스에서 '2204'로 조회된 국가코드)
		String[] movieTypeCdArr = request.getParameterValues("movieTypeCdArr")==null? null:request.getParameterValues("movieTypeCdArr");	//영화형태코드 배열 (공통코드서비스에서 '2201'로 조회된 영화형태코드)

		// 영화코드조회 서비스 호출 (boolean isJson, String curPage, String itemPerPage,String directorNm, String movieCd, String movieNm, String openStartDt,String openEndDt, String ordering, String prdtEndYear, String prdtStartYear, String repNationCd, String[] movieTypeCdArr)
	    String movieCdResponse = service.getMovieList(true, curPage, itemPerPage, movieNm, directorNm, openStartDt, openEndDt, prdtStartYear, prdtEndYear, repNationCd, movieTypeCdArr);
		
		// Json 라이브러리를 통해 Handling
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String,Object> result = mapper.readValue(movieCdResponse, HashMap.class);
		System.out.println("result 확인 : " + result); //이거 확인하면 삭제
 		
		// "movieListResult" 키 아래에 있는 "movieList" HashMap 가져오기
		HashMap<String, Object> movieListResult = (HashMap<String, Object>) result.get("movieListResult");
		ArrayList<HashMap<String, Object>> movieList = (ArrayList<HashMap<String, Object>>) movieListResult.get("movieList");
		
		JSONObject jo = new JSONObject(result);	
		if(jo != null) 
		{
			for (int i = 0; i < movieList.size(); i++) 
			{
				String movieCd = jo.getJSONObject("movieListResult").getJSONArray("movieList").getJSONObject(i).getString("movieCd");
				System.out.print(i+"번째 영화 코드 : " + movieCd + ", ");
				int movie_id = movieService.getMovieID(movieCd);
				System.out.print(i+"번째 영화 아이디 : " + movie_id + ", ");
				if(movie_id != 0) //movie_id를 가져오면 DB에서 posterPath 가져와서 JSON 내용에 추가하기
				{
					String movieId = String.valueOf(movie_id);
					String getPosterPath;
					HashMap<String, Object> movie = movieList.get(i);     //  i번째 영화
					
					MovieDTO dto = movieService.selectOneMovie(movieId);	
					getPosterPath = dto.getMoviePosterPath();
					if(getPosterPath == null || getPosterPath.equals("")) 
					{  //posterpath가 안 채워진 경우(없는 경우) << 이건 검증 필요함
						movie.put("posterPath", "/popcornpedia/resources/images/movie/ready300.png");
					}
					else {
					getPosterPath = "http://image.tmdb.org/t/p/w200"+dto.getMoviePosterPath();
					movie.put("posterPath", getPosterPath); //키 추가하기
					}
					System.out.println("담았으면 확인 " + getPosterPath + ", " );
				}
				else if(movie_id ==0) //movie_id를 못찾으면 준비중 이미지 띄우기(아직 DB에 없거나 일부러 삭제한 영화인 경우)
				{ 
					System.out.println("movie_id 못찾았음. else if 실행");
					HashMap<String, Object> movie = movieList.get(i);
					//여기부터 코드 새로 추가
					try //substring 때문에 예외발생해서 try, catch문 추가함
					{ 
						String movieInfo = service.getMovieInfo(true, movieCd);
						JSONObject obj = new JSONObject(movieInfo);	
						String openDt = obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getString("openDt");
						String movieName = obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getString("movieNm");
						String movieYear = openDt.substring(0, 4); 
						
						System.out.println("영화 제목 :" + movieName + ", 연도 : " + movieYear);
						// ▼ api 정보 불러오기
						OkHttpClient client = new OkHttpClient();
						Request reques = new Request.Builder()
						  .url("https://api.themoviedb.org/3/search/movie?query="+movieName+"&include_adult=true&language=ko-kr&page=1&year="+movieYear)
						  .get()
						  .addHeader("accept", "application/json")
						  .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJiY2E3YzVhYWUyM2NhMmEwODMwNDBmM2YxNzRmMTdhMiIsInN1YiI6IjY0ZWU5MDIxZTBjYTdmMDEwZGUyZDg2MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.zpKwr_Vqp_h8YepA8M1_F53PTXjCU0EIipT3dsjk0Tk")
						  .build();
	
						Response respons = client.newCall(reques).execute();
						String message = respons.body().string();
						JSONObject json = new JSONObject(message);
						
						int total_results = json.getInt("total_results");
						System.out.println("tmdb json 결과 : " + total_results);
						String posterPath = null ;
						String tempPosterPath = null;
						if(total_results != 0 ) //TMDB에 결과가 있는 경우 이미지 URL + 줄거리 받아오기
						{ 
							tempPosterPath = json.getJSONArray("results").getJSONObject(0).getString("poster_path");
							if(tempPosterPath == null || tempPosterPath.equals("") ) //그래도 혹시나 포스터 URL을 못 가져오는 경우
							{
									posterPath = "/popcornpedia/resources/images/movie/ready300.png";
							}
							else 
							{
								posterPath = "http://image.tmdb.org/t/p/w200"+tempPosterPath;
							}
							movie.put("posterPath", posterPath);	
						}
						else {
							posterPath = "/popcornpedia/resources/images/movie/ready300.png";
							movie.put("posterPath", posterPath);	
						}
						movie.put("movieYear", movieYear);
					}
					catch(Exception e) {
						
						e.printStackTrace();
					}
				
				} // else if(movie_id ==0) 끝
				} //for문 끝
			} //if(jo != null)  끝
		
		model.addAttribute("result", result);
		ModelAndView mav = new ModelAndView("/movie/kobisResultMovie");
		return mav;

	}
	
	
	// Kobis검색 결과에서 상세페이지 열면 영진위 정보 + TMDB 정보 가져와서 DB에 추가(수정)하기
	@RequestMapping(value = "/movie/movieInfoInsert.do")
	public ModelAndView getMovieInfoKobis(@RequestParam("movieCd") String movieCd, HttpServletRequest request, HttpServletResponse response, Model model) throws Exception{
		
		/* --------------------------------------------------movieCd로 영화진흥위원회 영화 정보 가져오기------------------------------*/
		String movieInfo = service.getMovieInfo(true, movieCd);
		System.out.println("상세정보 페이지 열기 위한 movieCd : " + movieCd);
		
		JSONObject obj = new JSONObject(movieInfo); 
		String movieNm = obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getString("movieNm").trim();
		String movieNmEn = obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getString("movieNmEn");				
		
		//개봉일은 날짜까지 나오기 때문에 4자리 수로 자름
		String openDt = obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getString("openDt");
		String movieYear = openDt.substring(0, 4); 
		
		String movieNation = obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getJSONArray("nations").getJSONObject(0).getString("nationNm");
		String movieGrade = null;
		if(obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getJSONArray("audits").isEmpty()) {movieGrade=null;}
		else {
			 movieGrade=obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getJSONArray("audits").getJSONObject(0).getString("watchGradeNm");
		}
		String showTm =  null;
		if(obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getString("showTm").isEmpty()) {showTm=null;}
		else{
			showTm = obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getString("showTm");	
		}
		String movieGenres = null;
		if(obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getJSONArray("genres").isEmpty()) {movieGenres=null;}
		else {
			movieGenres = obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getJSONArray("genres").getJSONObject(0).getString("genreNm");	
			for(int i=1; i<obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getJSONArray("genres").length(); i++){
				movieGenres += ","+obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getJSONArray("genres").getJSONObject(i).getString("genreNm");
			}	
		}
		String movieDirector = null; //감독이 비워져 있는 경우가 있어서 if문 추가
			if(obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getJSONArray("directors").isEmpty()) {movieDirector = null;}
			else 
			{
			 movieDirector = obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getJSONArray("directors").getJSONObject(0).getString("peopleNm");
					for(int i=1; i<obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getJSONArray("directors").length(); i++)
					{
						movieDirector += ","+obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getJSONArray("directors").getJSONObject(i).getString("peopleNm");
					}
			}
		String actors = null; //배우가 비워져 있는 경우가 있어서 if문 추가
			if(obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getJSONArray("actors").isEmpty()){actors=null;}
			else 
			{
				actors = obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getJSONArray("actors").getJSONObject(0).getString("peopleNm");
				for(int i=1; i<obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getJSONArray("actors").length(); i++) 
				{
					actors += ", " + obj.getJSONObject("movieInfoResult").getJSONObject("movieInfo").getJSONArray("actors").getJSONObject(i).getString("peopleNm");
				}
			}	
			
			//movieCd로 movie_id 검색하기 (DB에 있는지 확인)	
			String movie_id = String.valueOf(movieService.getMovieID(movieCd));
			MovieDTO dto = movieService.selectOneMovie(movie_id);
			String posterPath = null ;
			if( dto != null) {
				if(dto.getMoviePosterPath() != null) {
					posterPath="http://image.tmdb.org/t/p/w300"+dto.getMoviePosterPath();
					dto.setMoviePosterPath(posterPath);
					model.addAttribute("dto", dto);
				}
			}
			if(dto ==null || dto.getMoviePosterPath() == null || posterPath.equals("")) 
			{					
				/* --------------------------------------------------TMDB로 포스터, 배경화면 URL, 줄거리 가져오기------------------------------*/			
				//TMDB 정보 받아오기
				OkHttpClient client = new OkHttpClient();
				Request reques = new Request.Builder()
				  .url("https://api.themoviedb.org/3/search/movie?query="+movieNm+"&include_adult=true&language=ko-kr&page=1&year="+movieYear)
				  .get()
				  .addHeader("accept", "application/json")
				  .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJiY2E3YzVhYWUyM2NhMmEwODMwNDBmM2YxNzRmMTdhMiIsInN1YiI6IjY0ZWU5MDIxZTBjYTdmMDEwZGUyZDg2MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.zpKwr_Vqp_h8YepA8M1_F53PTXjCU0EIipT3dsjk0Tk")
				  .build();
	
				Response respons = client.newCall(reques).execute();
				String message = respons.body().string();
				JSONObject json = new JSONObject(message);
				
				int total_results = json.getInt("total_results");
				System.out.println("tmdb json 결과 : " + total_results);
				
					if(total_results != 0) 
					{ //TMDB에 결과가 있는 경우 이미지 URL + 줄거리 받아오기
						String backdropPath = null;
						try {
				            if (json.getJSONArray("results").getJSONObject(0).isNull("backdrop_path")) {
				            	backdropPath = null;
				            }
				            backdropPath = json.getJSONArray("results").getJSONObject(0).getString("backdrop_path");
				        } catch (Exception e) {
				            e.printStackTrace();
				        }
						String overView = json.getJSONArray("results").getJSONObject(0).getString("overview");
						posterPath = json.getJSONArray("results").getJSONObject(0).getString("poster_path");
						if(posterPath.equals(null) || posterPath.equals("") ) //그래도 혹시나 포스터 URL을 못 가져오는 경우
						{
							posterPath = "/popcornpedia/resources/images/movie/ready300.png";
						}
						else {
						dto = new MovieDTO(movieNm, movieNmEn, movieYear, movieGenres, movieNation, movieDirector, actors,  movieGrade, showTm, posterPath, backdropPath, overView, movieCd);
						movieService.insertMovie(dto); //dto는 dao의 insertMovie 메서드로 보내서 가져온 데이터 DB에 추가하기
						posterPath = "http://image.tmdb.org/t/p/w300"+json.getJSONArray("results").getJSONObject(0).getString("poster_path"); //뷰페이지로 보낼때는 이미지용 주소 추가
						dto.setMoviePosterPath(posterPath);
						model.addAttribute("dto", dto); //이미지까지 전부 모델 속성에 dto 담기
						}
					}
					else if(total_results == 0)
					{ //TMDB에 결과가 안 나오는 경우 (이미지, 줄거리가 없는 경우) 그냥 DTO에 영진위 정보 담음
						dto = new MovieDTO(movieNm, movieNmEn, movieYear, movieGenres, movieNation, movieDirector, actors,  movieGrade, showTm,  movieCd);
						movieService.insertMovie(dto);
						posterPath="/popcornpedia/resources/images/movie/ready300.png";
						dto.setMoviePosterPath(posterPath);
						model.addAttribute("dto", dto);
					}		
			}//if(posterPath == null || posterPath.equals("")) 끝
			
			ModelAndView mav = new ModelAndView("/movie/movieInfo");
			return mav;	
	}
	
	public static String normalize(String input) {
        if (input == null) return null;
        return input.replace("Ⅰ", "I")
                    .replace("Ⅱ", "II")
                    .replace("Ⅲ", "III")
                    .replace("Ⅳ", "IV")
                    .replace("Ⅴ", "V")
                    .replace("Ⅵ", "VI")
                    .replace("Ⅶ", "VII")
                    .replace("Ⅷ", "VIII")
                    .replace("Ⅸ", "IX")
                    .replace("Ⅹ", "X");
    }

}

package com.popcornpedia.admin.controllor;

import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import com.popcornpedia.member.dto.MemberDTO;

public interface AdminMemberController {
	
	public ModelAndView insertMember(@ModelAttribute("memberInfo") MemberDTO memberDTO, HttpServletRequest request, HttpServletResponse response) throws Exception;
	public ModelAndView selectAllMember(@RequestParam(defaultValue = "1") int num) throws Exception;
	public ModelAndView updateMember(@ModelAttribute("memberInfo") MemberDTO memberDTO, HttpServletRequest request, HttpServletResponse response) throws Exception;
	public ModelAndView updateMemberForm(@RequestParam("member_id") String member_id, HttpServletRequest request, HttpServletResponse response) throws Exception;
	public ModelAndView deleteMember(@RequestParam("member_id") String member_id, HttpServletRequest request, HttpServletResponse response) throws Exception;
	public ModelAndView searchMember(@RequestParam("searchType") String searchType,@RequestParam("keyword") String keyword,  HttpServletRequest request, HttpServletResponse response) throws Exception;
	public ModelAndView goMemberForm(HttpServletRequest request, HttpServletResponse response) throws Exception;

}

package com.springBoot.user.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.springBoot.exception.Exception;
import com.springBoot.response.Response;
import com.springBoot.response.ResponseToken;
import com.springBoot.user.dto.Colordto;
import com.springBoot.user.dto.Labeldto;
import com.springBoot.user.dto.Logindto;
import com.springBoot.user.dto.Maildto;
import com.springBoot.user.dto.Notedto;
import com.springBoot.user.dto.Userdto;
import com.springBoot.user.model.Label;
import com.springBoot.user.model.Note;
import com.springBoot.user.model.User;
@Service
public interface UserService 
{
	//register
	Response Register(Userdto userDto) throws Exception, UnsupportedEncodingException;

	//Login
	ResponseToken Login(Logindto loginDto) throws Exception, UnsupportedEncodingException;

	//verification of email
	Response validateEmailId(String token) throws Exception;

	//forgot password?
	Response forgetPassword(Maildto emailDto) throws Exception, UnsupportedEncodingException;

	//Authenticate user
	ResponseToken authentication(Optional<User> user, String password, String email,String token) 
			throws UnsupportedEncodingException, Exception;

	Response setpassword(String emailId, String password);

	//CRUD for Note
	Response Create(Notedto notedto, String token);

	List<Note> showNotes(String token);

	Response updatenote(String token, @Valid Notedto notedto, Long noteid);

	Response deletenote(String token, Long noteid);
	
	List<Note> showLabelsById(String token, Long labelid);

	//CRUD for Label
	Response CreateLabel(String token, @Valid Labeldto labeldto);
	
	List<Label> showLabels(String token);

	Response updateLabel(String token, Long labelid, @Valid Labeldto labeldto);
	
	Response deleteLabel(String token, Long labelid);

	Response labelForNote(String token, @Valid Labeldto labeldto, Long noteid);

	Response removeLabel(String token, Long labelid, Long noteid);

	Response labelToNote(String token, Long labelid, Long noteid);

	List<Label> showNotesById(String token, Long noteid);
	
	//Note operations

	Response colorToNote(String token, Colordto colordto, Long noteid);

	Response trashNote(String token, Long noteid);

	Response archiveUnarhiveNote(String token, Long noteid);

	Response pinNote(String token, Long noteid);

	List<Note> showPinnedNotes(String token);

	List<Note> showArchiveNotes(String token);

	List<Note> showTrashNotes(String token);

	List<Note> showUnpinNotes(String token);

	List<Note> showUnarchiveNotes(String token);

	List<Note> showUntrashNotes(String token);

	//Profile Pic
	Response uploadProfilePic(String token, MultipartFile image) throws IOException;

	Resource profilePic(String token) throws MalformedURLException;

	Response uploadImageToNote(String token, MultipartFile image, Long noteid) throws IOException;

	Resource noteImages(String token, Long noteid) throws MalformedURLException;


	
	
}
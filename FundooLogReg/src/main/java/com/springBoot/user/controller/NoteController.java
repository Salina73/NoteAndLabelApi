package com.springBoot.user.controller;

import com.springBoot.exception.Exception;
import com.springBoot.user.dto.Colordto;
import com.springBoot.user.dto.Notedto;
import com.springBoot.user.model.Label;
import com.springBoot.user.model.Note;
import com.springBoot.user.repository.NoteRepository;
import com.springBoot.user.repository.UserRepo;
import com.springBoot.response.Response;
import com.springBoot.user.service.UserService;
import com.springBoot.utility.TokenGeneration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/user")
public class NoteController {

	@Autowired
	NoteRepository noteRepository;

	@Autowired
	UserRepo userRepo;

	@Autowired
	UserService userService;

	@Autowired
	TokenGeneration token1;

	@Autowired
	Response statusResponse;

	Note updatedNote;

	// Create a new Note
	@PostMapping("/createNote")
	public ResponseEntity<Response> createNote(@RequestHeader String token, @Valid @RequestBody Notedto notedto)
			throws Exception, UnsupportedEncodingException {
		Response response = userService.Create(notedto, token);
		System.out.println(response);
		return new ResponseEntity<>(response, HttpStatus.OK);

	}

	// Get All Notes
	@GetMapping("/getAllNotes/")
	public List<Note> getAllNotes(@RequestHeader String token) {
		List<Note> allNotes = userService.showNotes(token);
		return allNotes;

	}

	// Update a Note
	@PutMapping("/updateNote/")
	public ResponseEntity<Response> updateNote(@RequestHeader String token, @Valid @RequestBody Notedto notedto,
			@RequestParam Long noteid) {
		Response response = userService.updatenote(token, notedto, noteid);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	// Delete a Note
	@DeleteMapping("/deleteNote/")
	public ResponseEntity<Response> deleteNote(@RequestHeader String token, @RequestParam Long noteid) {
		Response response = userService.deletenote(token, noteid);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping("/getLabelsByNoteid/")
	public List<Label> getLabelsByNoteid(@RequestHeader String token, @RequestParam Long noteid) {
		List<Label> allLabels = userService.showNotesById(token, noteid);
		return allLabels;

	}

	// Add color to note
	@PutMapping("/colorNote/")
	public ResponseEntity<Response> colorNote(@RequestHeader String token, @RequestBody Colordto colordto,
			@RequestParam Long noteid) {
		Response response = userService.colorToNote(token, colordto, noteid);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	// Pin & Unpin notes
	@PutMapping("/pinAndUnpinNote/")
	public ResponseEntity<Response> pinAndUnpinNote(@RequestHeader String token, @RequestParam Long noteid) {
		Response response = userService.pinNote(token, noteid);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	// archive & unarhive note
	@PutMapping("/archiveNote/")
	public ResponseEntity<Response> archiveNote(@RequestHeader String token, @RequestParam Long noteid) {
		Response response = userService.archiveUnarhiveNote(token, noteid);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	// Trash & untrash note
	@PutMapping("/trashUntrashNote/")
	public ResponseEntity<Response> trashUntrashNote(@RequestHeader String token, @RequestParam Long noteid) {
		Response response = userService.trashNote(token, noteid);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	// Show pin notes
	@GetMapping("/getPinnedNotes/")
	public List<Note> getPinnedNotes(@RequestHeader String token) {
		List<Note> allNotes = userService.showPinnedNotes(token);
		return allNotes;

	}

	// Show unPin notes
	@GetMapping("/getUnpinnedNotes/")
	public List<Note> getUnpinnedNotes(@RequestHeader String token) {
		List<Note> allNotes = userService.showUnpinNotes(token);
		return allNotes;

	}

	// Show archive notes
	@GetMapping("/getArchiveNotes/")
	public List<Note> getArchiveNotes(@RequestHeader String token) {
		List<Note> allNotes = userService.showArchiveNotes(token);
		return allNotes;

	}

	// Show unarchive notes
	@GetMapping("/getUnarchiveNotes/")
	public List<Note> getUnarchiveNotes(@RequestHeader String token) {
		List<Note> allNotes = userService.showUnarchiveNotes(token);
		return allNotes;

	}

	// Show trash notes
	@GetMapping("/getTrashNotes/")
	public List<Note> getTrashNotes(@RequestHeader String token) {
		List<Note> allNotes = userService.showTrashNotes(token);
		return allNotes;

	}
	//Show untrash notes
	@GetMapping("/getUntrashNotes/")
	public List<Note> getUntrashNotes(@RequestHeader String token) {
		List<Note> allNotes = userService.showUntrashNotes(token);
		return allNotes;

	}
	@PostMapping("/noteImage/")
	public ResponseEntity<Response> noteImage(@RequestHeader String token,@RequestParam MultipartFile image,@RequestParam Long noteid) throws IOException
	{
		Response response = userService.uploadImageToNote(token,image,noteid);
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}
	
	@GetMapping("/getNoteImage/")
	public ResponseEntity<Resource> getNoteImage(@RequestHeader String token,@RequestParam Long noteid) throws MalformedURLException
	{
		Resource response = userService.noteImages(token,noteid);
		return new ResponseEntity<Resource>(response, HttpStatus.OK);
	}
}
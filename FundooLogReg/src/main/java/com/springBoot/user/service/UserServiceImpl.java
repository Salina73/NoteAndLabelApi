package com.springBoot.user.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.JavaFileManager.Location;
import javax.validation.Valid;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.springBoot.elasticSearch.ElasticSearchOfNote;
import com.springBoot.exception.Exception;
import com.springBoot.rabbit.MessageListener;
import com.springBoot.rabbit.RabbitMqProducer;
import com.springBoot.response.Response;
import com.springBoot.response.ResponseToken;
import com.springBoot.user.dto.Collaboratordto;
import com.springBoot.user.dto.Colordto;
import com.springBoot.user.dto.Labeldto;
import com.springBoot.user.dto.Logindto;
import com.springBoot.user.dto.Maildto;
import com.springBoot.user.dto.Notedto;
import com.springBoot.user.dto.Userdto;
import com.springBoot.user.model.Collaborator;
import com.springBoot.user.model.Label;
import com.springBoot.user.model.Note;
import com.springBoot.user.model.User;
import com.springBoot.user.repository.CollaboratorRepository;
import com.springBoot.user.repository.LabelRepository;
import com.springBoot.user.repository.NoteRepository;
import com.springBoot.user.repository.UserRepo;

import com.springBoot.utility.ResponseHelper;
import com.springBoot.utility.TokenGeneration;
import com.springBoot.utility.Utility;
import org.springframework.util.StringUtils;

@Component
@SuppressWarnings("unused")
@PropertySource("classpath:message.properties")
@Service("userService")
public class UserServiceImpl implements UserService {

	@Autowired
	private UserRepo userRepo;

	@Autowired
	private NoteRepository noteRepository;

	@Autowired
	private LabelRepository labelRepository;

	@Autowired
	private CollaboratorRepository collaboratorRepository;

	@Autowired
	private ModelMapper modelMapper;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TokenGeneration tokenUtil;

	@Autowired
	TokenGeneration token1;

	@Autowired
	private Response statusResponse;
	@Autowired
	private Utility utility;

	@Autowired
	private Environment environment;

	@Autowired
	private ElasticSearchOfNote elasticSearch;

	@Autowired
	private RedisTemplate<String,Object> redisTemp;

	@Value("${key}")
	private String key;
	
	@Value("${profilePath}")
	private final Path filePath=null;

	private final Path noteImagePath=null;
	
	@Autowired 
	private RabbitMqProducer rabbitMqProducer;
	
	public Response register(Userdto userDto) {
		User user = modelMapper.map(userDto, User.class);
		Optional<User> alreadyPresent = userRepo.findByEmailId(user.getEmailId());
		if (alreadyPresent.isPresent()) {
			throw new Exception(environment.getProperty("status.register.emailExistError"));
		}

		String password = passwordEncoder.encode(userDto.getPassword());
		user.setPassword(password);
		user = userRepo.save(user);
		String token1 = tokenUtil.createToken(user.getUserId());
				
		rabbitMqProducer.sendSimpleMessage("Your mail is sent!!!",user.getEmailId(), "Verification is required for login.Please click below link.\n",
				"http://localhost:8080/user/" + token1);
		
		//Utility.sendToken(user.getEmailId(), "Verification is required for login.Please click below link.\n",
			//	"http://localhost:8080/user/" + token1);
		
		statusResponse = ResponseHelper.statusResponse(200, "register successfully");
		return statusResponse;
	}

	public ResponseToken login(Logindto loginDto) {

		Optional<User> user = userRepo.findByEmailId(loginDto.getEmailId());

		ResponseToken response = new ResponseToken();
		if (user.isPresent()) {
			String token = tokenUtil.createToken(user.get().getUserId());
			System.out.println("password..." + (loginDto.getPassword()));
			
			List<String> n=new ArrayList<String>();
			n.add(user.get().getEmailId());
			n.add(user.get().getFirstName());
			n.add(user.get().getLastName());
			n.add(user.get().getProfilePic());
			redisTemp.opsForValue().set(key, n);
			
			return authentication(user, loginDto.getPassword(), loginDto.getEmailId(), token);
		} else {
			response.setStatusCode(404);
			response.setStatusMessage("User not found");
			return response;
		}

	}

	public ResponseToken authentication(Optional<User> user, String password, String email, String token) {

		ResponseToken response = new ResponseToken();
		if (!user.get().isVerify()) {
			response.setStatusCode(401);
			response.setStatusMessage(environment.getProperty("user.login.verification"));
			return response;
		}

		else if (user.get().isVerify()) {
			boolean status = passwordEncoder.matches(password, user.get().getPassword());
			System.out.println("status" + status);
			if (status == true) {
				System.out.println("logged in");
				response.setStatusCode(200);
				response.setToken(token);
				response.setStatusMessage(environment.getProperty("user.login"));
				return response;
			} else if (status == false) {
				response.setStatusCode(201);
				response.setStatusMessage("Incorrect password");
				return response;
			}
		}
		throw new Exception(401, environment.getProperty("user.login.verification"));
	}

	public Response validateEmailId(String token) {
		System.out.println("Hi");
		Long id = tokenUtil.decodeToken(token);
		User user = userRepo.findById(id)
				.orElseThrow(() -> new Exception(404, environment.getProperty("user.validation.email")));
		user.setVerify(true);
		userRepo.save(user);
		statusResponse = ResponseHelper.statusResponse(200, environment.getProperty("user.validation"));
		return statusResponse;
	}

	public Response forgetPassword(Maildto emailDto) throws Exception, UnsupportedEncodingException {
		Optional<User> alreadyPresent = userRepo.findByEmailId(emailDto.getEmailId());

		if (!alreadyPresent.isPresent()) {
			throw new Exception(401, environment.getProperty("user.forgetPassword.emailId"));
		} else {
			Utility.send(emailDto.getEmailId(), "Click below link to reset password",
					" http://localhost:4200/Reset-Password");
		}
		return ResponseHelper.statusResponse(200, environment.getProperty("user.forgetpassword.link"));
	}

	@Override
	public Response setpassword(String emailId, String password) {
		Optional<User> alreadyPresent = userRepo.findByEmailId(emailId);

		if (!alreadyPresent.isPresent()) {
			throw new Exception(401, environment.getProperty("user.forgetPassword.emailId"));
		} else {
			Long id = alreadyPresent.get().getUserId();

			User user = userRepo.findById(id).orElseThrow(() -> new Exception("User not found!!"));

			String password1 = passwordEncoder.encode(password);

			user.setPassword(password1);
			userRepo.save(user);
			return ResponseHelper.statusResponse(200, "Password set successfully...");
		}
	}

	@Override
	public Response uploadProfilePic(String token, MultipartFile image) throws IOException {
		Long id = token1.decodeToken(token);
		Optional<User> user = userRepo.findById(id);
		if (!user.isPresent())
			throw new Exception(404, "User not present");
		else {
			String filename = StringUtils.cleanPath(image.getOriginalFilename());
			try {
				Files.copy(image.getInputStream(), filePath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
				user.get().setProfilePic((filePath.resolve(filename)).toString());
				userRepo.save(user.get()); 
				List<String> n=new ArrayList<String>();
				n.add(user.get().getEmailId());
				n.add(user.get().getFirstName());
				n.add(user.get().getLastName());
				n.add(user.get().getProfilePic());
				redisTemp.opsForValue().set(key, n);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ResponseHelper.statusResponse(200, "Profile picture is successfully uploaded");
	}

	@Override
	public Resource profilePic(String token) throws MalformedURLException {
		Long id = token1.decodeToken(token);
		User user = userRepo.findById(id).get();
		Path imagePath = filePath.resolve(user.getProfilePic());
		Resource imageResource = new UrlResource(imagePath.toUri());
		return imageResource;
	}
	@Override
	public List<String> showProfile() {
		return (List<String>) redisTemp.opsForValue().get(key);
	}


	// CRUD for note

	@Override
	public Response create(Notedto notedto, String token) {
		Note note = modelMapper.map(notedto, Note.class);
		ResponseToken response = new ResponseToken();
		Long id = token1.decodeToken(token);
		note.setUserid(id);

		Optional<User> user = userRepo.findById(id);
		((List<Note>) user.get().getNotes()).add(note);

		noteRepository.save(note);
		userRepo.save(user.get());

		elasticSearch.createNote(note);
		return ResponseHelper.statusResponse(200, "Note added successfully");
	}

	@Override
	public List<Note> showNotes(String token) {
		Long id = token1.decodeToken(token);
		User user = userRepo.findById(id).get();
		return (List<Note>) user.getNotes();
	}

	@Override
	public Response updatenote(String token, @Valid Notedto notedto, Long noteid) {
		Long id = token1.decodeToken(token);
		Note note = noteRepository.findByUseridAndNoteid(id, noteid);
		note.setTitle(notedto.getTitle());
		note.setDescription(notedto.getDescription());
		noteRepository.save(note);
		elasticSearch.updateNote(note, note.getNoteid());
		return ResponseHelper.statusResponse(200, "Note updated successfully");
	}

	public Response deletenote(String token, Long noteid) {
		Long id = token1.decodeToken(token);
		Note note = noteRepository.findById(noteid).orElseThrow(null);
		// Optional<Note> notes = noteRepository.findById(noteid);
		// ((List<Label>) notes.get().getLabel()).remove(notes.get().getLabel());

		// (notes.get().getUserColaborators()).remove(notes.get().getUserColaborators());

		Optional<User> user = userRepo.findById(id);
		((List<Note>) user.get().getNotes()).remove(note);

		userRepo.save(user.get());
		noteRepository.delete(note);
		elasticSearch.deleteNote(note.getNoteid());
		return ResponseHelper.statusResponse(200, "Note deleted successfully");
	}

	@Override
	public List<Label> showNotesById(String token, Long noteid) {
		Long id = token1.decodeToken(token);
		Note note = noteRepository.findByUseridAndNoteid(id, noteid);
		return (List<Label>) note.getLabel();
	}

	@Override
	public Response colorToNote(String token, Colordto colordto, Long noteid) {
		Long id = token1.decodeToken(token);
		Note note = noteRepository.findByUseridAndNoteid(id, noteid);
		note.setColor(colordto.getColor());
		noteRepository.save(note);
		return ResponseHelper.statusResponse(200, "Color is added successfully to note");
	}

	@Override
	public Response trashNote(String token, Long noteid) {
		Long id = token1.decodeToken(token);
		Note note = noteRepository.findByUseridAndNoteid(id, noteid);
		if (!note.isTrashStatus()) {
			note.setTrashStatus(true);
			if (note.isPinStatus())
				note.setPinStatus(false);

			if (note.isArchiveStatus())
				note.setArchiveStatus(false);

			if (note.getTime() != null)
				note.setTime(null);

			noteRepository.save(note);
			return ResponseHelper.statusResponse(200, "Note is in trash");
		} else if (note.isTrashStatus() == true) {
			note.setTrashStatus(false);
			noteRepository.save(note);
			return ResponseHelper.statusResponse(200, "Note is restore");
		}
		return statusResponse;
	}

	@Override
	public Response archiveUnarhiveNote(String token, Long noteid) {
		Long id = token1.decodeToken(token);
		Note note = noteRepository.findByUseridAndNoteid(id, noteid);
		if (!note.isArchiveStatus()) {
			note.setArchiveStatus(true);
			if (note.isPinStatus())
				note.setPinStatus(false);

			noteRepository.save(note);
			return ResponseHelper.statusResponse(200, "Note is archive");
		} else if (note.isTrashStatus()) {
			note.setArchiveStatus(false);
			noteRepository.save(note);
			return ResponseHelper.statusResponse(200, "Note is restore");
		}
		return statusResponse;
	}

	@Override
	public Response pinNote(String token, Long noteid) {
		Long id = token1.decodeToken(token);
		Note note = noteRepository.findByUseridAndNoteid(id, noteid);
		if (!note.isPinStatus()) {
			note.setPinStatus(true);
			if (note.isArchiveStatus())
				note.setArchiveStatus(false);

			noteRepository.save(note);
			return ResponseHelper.statusResponse(200, "Note is pinned");
		} else if (note.isPinStatus()) {
			note.setPinStatus(false);
			noteRepository.save(note);
			return ResponseHelper.statusResponse(200, "Note is unpinned");
		}
		return statusResponse;
	}

	@Override
	public List<Note> showPinnedNotes(String token) {
		List<Note> pin = new ArrayList<Note>();
		Long id = token1.decodeToken(token);
		User user = userRepo.findById(id).get();
		List<Note> notes = (List<Note>) user.getNotes();
		for (Note note : notes) {
			if (note.isPinStatus())
				pin.add(note);
		}
		return pin;
	}

	@Override
	public List<Note> showArchiveNotes(String token) {
		List<Note> archive = new ArrayList<Note>();
		Long id = token1.decodeToken(token);
		User user = userRepo.findById(id).get();
		List<Note> notes = (List<Note>) user.getNotes();
		for (Note note : notes) {
			if (note.isArchiveStatus())
				archive.add(note);
		}
		return archive;
	}

	@Override
	public List<Note> showTrashNotes(String token) {
		List<Note> trash = new ArrayList<Note>();
		Long id = token1.decodeToken(token);
		User user = userRepo.findById(id).get();
		List<Note> notes = (List<Note>) user.getNotes();
		for (Note note : notes) {
			if (note.isTrashStatus())
				trash.add(note);
		}
		return trash;
	}

	@Override
	public List<Note> showUnpinNotes(String token) {
		List<Note> unPin = new ArrayList<Note>();
		Long id = token1.decodeToken(token);
		User user = userRepo.findById(id).get();
		List<Note> notes = (List<Note>) user.getNotes();
		for (Note note : notes) {
			if (!note.isPinStatus())
				unPin.add(note);
		}
		return unPin;
	}

	@Override
	public List<Note> showUnarchiveNotes(String token) {
		List<Note> unArchive = new ArrayList<Note>();
		Long id = token1.decodeToken(token);
		User user = userRepo.findById(id).get();
		List<Note> notes = (List<Note>) user.getNotes();
		for (Note note : notes) {
			if (!note.isArchiveStatus())
				unArchive.add(note);
		}
		return unArchive;
	}

	@Override
	public List<Note> showUntrashNotes(String token) {
		List<Note> unTrash = new ArrayList<Note>();
		Long id = token1.decodeToken(token);
		User user = userRepo.findById(id).get();
		List<Note> notes = (List<Note>) user.getNotes();
		for (Note note : notes) {
			if (!note.isTrashStatus())
				unTrash.add(note);
		}
		return unTrash;
	}

	@Override
	public Response uploadImageToNote(String token, MultipartFile image, Long noteid) throws IOException {
		Long id = token1.decodeToken(token);
		Note note = noteRepository.findByUseridAndNoteid(id, noteid);
		String filename = StringUtils.cleanPath(image.getOriginalFilename());
		try {
			Files.copy(image.getInputStream(), noteImagePath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
			note.setNoteImages(filename);
			noteRepository.save(note);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ResponseHelper.statusResponse(200, "Note image is set");
	}

	@Override
	public Resource noteImages(String token, Long noteid) throws MalformedURLException {
		Long id = token1.decodeToken(token);
		Note note = noteRepository.findByUseridAndNoteid(id, noteid);
		Path imagePath = noteImagePath.resolve(note.getNoteImages());
		Resource imageResource = new UrlResource(imagePath.toUri());
		return imageResource;
	}

	@Override
	public Response setReminder(String token, Long noteid, LocalDateTime time) {
		Long id = token1.decodeToken(token);
		Note note = noteRepository.findByUseridAndNoteid(id, noteid);
		LocalDateTime date = LocalDateTime.now();
		if (date.compareTo(time) < 0) {
			note.setTime(time);
			noteRepository.save(note);
			return ResponseHelper.statusResponse(200, "Reminder is set to note");
		}
		return ResponseHelper.statusResponse(404, "Reminder is not set to note");
	}

	@Override
	public Response discardReminder(String token, Long noteid) {
		Long id = token1.decodeToken(token);
		Note note = noteRepository.findByUseridAndNoteid(id, noteid);
		note.setTime(null);
		noteRepository.save(note);
		return ResponseHelper.statusResponse(200, "Reminder is deleted");
	}

	@Override
	public LocalDateTime viewReminder(String token, Long noteid) {
		Long id = token1.decodeToken(token);
		Note note = noteRepository.findByUseridAndNoteid(id, noteid);
		return note.getTime();
	}

	@Override
	public Response checkingReminder(String token, Long noteid) {
		Long id = token1.decodeToken(token);
		Note note = noteRepository.findByUseridAndNoteid(id, noteid);
		LocalDateTime date = LocalDateTime.now();
		if (date.compareTo(note.getTime()) > 0) {
			note.setTime(null);
			noteRepository.save(note);
			return ResponseHelper.statusResponse(200, "Reminder is removed");
		}
		return statusResponse;
	}

	// Collaborator
	@Override
	public Response addCollaboratorsToNote(String token, Long noteid, Collaboratordto collabDto) {
		Long id = token1.decodeToken(token);
		Note note = noteRepository.findByUseridAndNoteid(id, noteid);
		Optional<User> user = userRepo.findById(id);
		if (user.get().getEmailId() == collabDto.getEmail())
			return ResponseHelper.statusResponse(401, "This email already exists");

		Optional<Collaborator> collabs = collaboratorRepository.findByEmailAndNoteEntityId(collabDto.getEmail(),
				noteid);

		if (collabs.isPresent())
			return ResponseHelper.statusResponse(402, "This email already exists");

		Collaborator collab = modelMapper.map(collabDto, Collaborator.class);
		Optional<User> users = userRepo.findByEmailId(collab.getEmail());
		if (users.isPresent()) {
			collab.setNoteEntityId(noteid);
			collab.setUserEntityId(id);
			((List<Collaborator>) user.get().getCollaborators()).add(collab);

			((List<Collaborator>) note.getUserColaborators()).add(collab);

			collaboratorRepository.save(collab);
			userRepo.save(user.get());
			noteRepository.save(note);
			Utility.sendMailForCollaboration(collabDto.getEmail(), "Click here for collaboration",
					"\n" + note.getTitle() + "/n" + note.getDescription());
			return ResponseHelper.statusResponse(200, "Collaborator is added");
		}

		return ResponseHelper.statusResponse(404, "Collaborator is not added");
	}

	@Override
	public Response removeCollaboratorFromNote(String token, Long noteid, Collaboratordto collabDto) {
		Long id = token1.decodeToken(token);
		Note note = noteRepository.findByUseridAndNoteid(id, noteid);
		Collaborator collab = collaboratorRepository.findByEmailAndNoteEntityId(collabDto.getEmail(), noteid).get();
		((List<Collaborator>) note.getUserColaborators()).remove(collab);

		User users = userRepo.findById(id).get();
		((List<Collaborator>) users.getCollaborators()).remove(collab);

		noteRepository.save(note);
		userRepo.save(users);
		collaboratorRepository.delete(collab);

		Optional<Collaborator> collabs = collaboratorRepository.findByEmailAndNoteEntityId(collabDto.getEmail(),
				noteid);
		if (!collabs.isPresent()) {
			Optional<User> user = userRepo.findById(id);
			((List<Collaborator>) user.get().getCollaborators()).remove(collab);
			userRepo.save(user.get());
		}
		return ResponseHelper.statusResponse(200, "Collaborator is removed from note");
	}

	@Override
	public List<Collaborator> collaboratorOfNote(String token, Long noteid) {
		Long id = token1.decodeToken(token);
		Note note = noteRepository.findByUseridAndNoteid(id, noteid);
		return (List<Collaborator>) note.getUserColaborators();
	}

	@Override
	public List<Collaborator> collaboratorOfUser(String token) {
		Long id = token1.decodeToken(token);
		User user = userRepo.findById(id).get();
		return (List<Collaborator>) user.getCollaborators();
	}

	// CRUD for Label
	@Override
	public Response createLabel(String token, @Valid Labeldto labeldto) {

		Optional<Label> labels = labelRepository.findByLabelName(labeldto.getLabelName());

		if (labels.isPresent()) {
			return ResponseHelper.statusResponse(401, "Duplicate label.");
		}

		Label label = modelMapper.map(labeldto, Label.class);

		ResponseToken response = new ResponseToken();
		Long id = token1.decodeToken(token);
		label.setUserId(id);

		Optional<User> user = userRepo.findById(id);
		((List<Label>) user.get().getLabel()).add(label);

		labelRepository.save(label);
		userRepo.save(user.get());

		return ResponseHelper.statusResponse(200, "Label added successfully");
	}

	@Override
	public List<Label> showLabels(String token) {
		Long id = token1.decodeToken(token);
		User user = userRepo.findById(id).get();
		return (List<Label>) user.getLabel();
	}

	@Override
	public Response updateLabel(String token, Long labelid, @Valid Labeldto labeldto) {

		Optional<Label> labels = labelRepository.findByLabelName(labeldto.getLabelName());

		if (labels.isPresent()) {
			return ResponseHelper.statusResponse(401, "Duplicate label.");
		} else {
			Long id = token1.decodeToken(token);
			Label label = labelRepository.findByLabelidAndUserId(labelid, id);
			label.setLabelName(labeldto.getLabelName());
			labelRepository.save(label);
			return ResponseHelper.statusResponse(200, "Label updated successfully");
		}
	}

	@Override
	public Response deleteLabel(String token, Long labelid) {
		Long id = token1.decodeToken(token);
		Optional<User> user = userRepo.findById(id);
		Label label = labelRepository.findById(labelid).orElse(null);
		((List<Label>) user.get().getLabel()).remove(label);
		userRepo.save(user.get());
		labelRepository.delete(label);
		return ResponseHelper.statusResponse(200, "Label deleted successfully");
	}

	@Override
	public Response labelForNote(String token, @Valid Labeldto labeldto, Long noteid) {
		Optional<Label> labels = labelRepository.findByLabelName(labeldto.getLabelName());

		if (labels.isPresent()) {
			return ResponseHelper.statusResponse(401, "Duplicate label.");
		}

		Label label = modelMapper.map(labeldto, Label.class);

		Long id = token1.decodeToken(token);
		label.setUserId(id);
		label.setNoteid(noteid);

		Optional<User> user = userRepo.findById(id);
		((List<Label>) user.get().getLabel()).add(label);

		Optional<Note> note = noteRepository.findById(noteid);
		((List<Label>) note.get().getLabel()).add(label);

		labelRepository.save(label);
		noteRepository.save(note.get());
		userRepo.save(user.get());

		List<Note> note1 = new ArrayList<Note>();
		note1.add(note.get());
		label.setNote(note1);
		labelRepository.save(labels.get());

		return ResponseHelper.statusResponse(200, "Label added successfully");

	}

	@Override
	public Response removeLabel(String token, Long labelid, Long noteid) {
		Long id = token1.decodeToken(token);
		Label label = labelRepository.findByLabelidAndUserId(labelid, id);

		Optional<Note> note = noteRepository.findById(noteid);
		((List<Label>) note.get().getLabel()).remove(label);

		((List<Note>) label.getNote()).remove(note.get());

		label.setNoteid(null);

		noteRepository.save(note.get());
		labelRepository.save(label);

		return ResponseHelper.statusResponse(200, "Label removed from note successfully");
	}

	@Override
	public Response labelToNote(String token, Long labelid, Long noteid) {
		Long id = token1.decodeToken(token);
		Label label = labelRepository.findByLabelidAndUserId(labelid, id);
		Note notes = noteRepository.findByUseridAndNoteid(id, noteid);

		label.setNoteid(noteid);

		Optional<Note> note = noteRepository.findById(noteid);
		((List<Label>) note.get().getLabel()).add(label);

		Optional<Label> labels = labelRepository.findById(labelid);
		((List<Note>) labels.get().getNote()).add(notes);
		labelRepository.save(label);
		noteRepository.save(note.get());
		labelRepository.save(labels.get());

		return ResponseHelper.statusResponse(200, "Label attached successfully");
	}

	@Override
	public List<Note> showLabelsById(String token, Long labelid) {
		Long id = token1.decodeToken(token);
		Label label = labelRepository.findByLabelidAndUserId(labelid, id);
		return (List<Note>) label.getNote();
	}
}
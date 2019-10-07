package com.springBoot.user.model;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Entity
@Table
@Data

public class User
{
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long userId;
	private String firstName;
	private String lastName;
	private String emailId;
	private String password;
	private String mobileNum;
	private boolean isVerify;
	
	private String profilePic;
	@JsonIgnore
	@OneToMany(fetch = FetchType.LAZY, cascade =  CascadeType.ALL)
    private List<Note> notes;
	
	@JsonIgnore
	@OneToMany(fetch = FetchType.LAZY, cascade =  CascadeType.ALL)
    private List<Label> label;
	
	@JsonIgnore
	@OneToMany(fetch = FetchType.LAZY, cascade =  CascadeType.ALL)
    private List<Collaborator> collaborators;
	
	private LocalDateTime dateTime = LocalDateTime.now();
	
	public List<Collaborator> getCollaborators() {
		return collaborators;
	}
	public void setCollaborators(List<Collaborator> collaborators) {
		this.collaborators = collaborators;
	}
	public List<Label> getLabel() {
		return label;
	}
	public void setLabel(List<Label> label) {
		this.label = label;
	}
	public List<Note> getNotes() 
	{
		return notes;
	}
	public void setNotes(List<Note> notes) 
	{
		this.notes = notes;
	}
	public Long getUserId() {
		return userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getEmailId() {
		return emailId;
	}
	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getMobileNum() {
		return mobileNum;
	}
	public void setMobileNum(String mobileNum) {
		this.mobileNum = mobileNum;
	}
	public boolean isVerify() {
		return isVerify;
	}
	public void setVerify(boolean isVerify) {
		this.isVerify = isVerify;
	}
	public LocalDateTime getRegisterDate() {
		return dateTime;
	}
	public void setRegisterDate(LocalDateTime dateTime) {
		this.dateTime = dateTime;
	}

	public String getProfilePic() {
		return profilePic;
	}
	public void setProfilePic(String path) {
		this.profilePic = path;
	}
}

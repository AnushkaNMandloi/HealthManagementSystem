package com.edu.neu.INFO7255Demo2.model;

public class AuthResponse {

	public AuthResponse(String Token) {
		super();
		this.Token = Token;
	}

	private String Token;

	public String getToken() {
		return Token;
	}

	public void setToken(String Token) {
		this.Token = Token;
	}

}

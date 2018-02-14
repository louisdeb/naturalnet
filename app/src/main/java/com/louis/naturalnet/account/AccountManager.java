package com.louis.naturalnet.account;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class AccountManager {

	private String url = "https://accounts.organicity.eu/realms/organicity/protocol/openid-connect/auth/?client_id=oppnet-dev&redirect_uri=oppnet%3A%2F%2Ftoken&response_type=token";

	private static AccountManager _obj = null;

	private String token;

	private int state;

	private final static String TAG = "AccountManager";

	private Claims parsedInfo;
	
	public final static int STATE_NOT_AUTHORISED = 0;
	public final static int STATE_AUTHORISED = 1;
	public final static int STATE_TOKEN_EXPIRED = 2;

	InputStream is;

	private AccountManager(){
		state = STATE_NOT_AUTHORISED;
		this.is = Thread.currentThread().getContextClassLoader().getResourceAsStream("accounts.organicity.eu.cert.pem");

		// This is needed within Eclipse or within a JAR
		if(this.is == null) {
			this.is = this.getClass().getResourceAsStream("accounts.organicity.eu.cert.pem");
		}
		
		if(this.is == null) {
			Log.d(TAG, "Certificate not found!");
		}
	}

	public static AccountManager getManager(){
		if(_obj == null){
			_obj = new AccountManager();
		}
		return _obj;
	}

	public void login(Context context){
		Uri uri = Uri.parse(url);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		context.startActivity(intent);
	}

	public void storeToken(String token){
		this.token = token;
		state = STATE_AUTHORISED;

		try {
			parsedInfo = parseJWT(token);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * This reads the public key from the certificate 
	 */
	private PublicKey getPublicKeyFromCert() {
		PublicKey pk = null;

		CertificateFactory f = null;
		try {
			f = CertificateFactory.getInstance("X.509");
			X509Certificate certificate = (X509Certificate) f.generateCertificate(this.is);
			pk = certificate.getPublicKey();
		} catch (CertificateException e) {
			System.err.println("CERTIFICATE NOT FOUND");
		}
		return pk;
	}
	/**
	 * 
	 * @param jwt A BASE64 encoded JWT
	 * @return A Claims object, which can be used 
	 * @throws Exception If the JWT is not valid or expired, this exception is thrown
	 */
	public Claims parseJWT(String jwt) throws Exception{
		PublicKey pk = getPublicKeyFromCert();
		return Jwts.parser().setSigningKey(pk).parseClaimsJws(jwt).getBody();
	}

	public String getToken(){
		return this.token;
	}
	
	public Claims getParsedInfo(){
		return parsedInfo;
	}

	public int getState(){
		return state;
	}
}

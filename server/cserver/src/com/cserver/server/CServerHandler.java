package com.cserver.server;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import com.cserver.shared.INSServerHandler;
import com.cserver.shared.Json;
import com.cserver.shared.SLogger;

public class CServerHandler implements INSServerHandler {

	private static final String TAG = "ServerHandleRequest";
	
	@Override
	public byte[] handle(byte[] input) {
		// TODO Auto-generated method stub
		
		//SLogger.d(TAG, "handle:input=" + Utils.bytesToHex(input));
		//JRealClock clock = new JRealClock();
		//clock.start();
		String inputS = null;
		try {
			inputS = new String(input, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			SLogger.exception(TAG, e);
		}
		
		if (inputS == null) {
			SLogger.e(TAG, "no input request decoded");
			return null;
		}
		//SLogger.d(TAG, "inputS=" + inputS);
		
		ClientRequest request = new ClientRequest();
		request.parseMap(Json.stringToMap(inputS));
		ClientRequest response = handleRequest(request);
		if (response == null) {
			SLogger.e(TAG, "no response");
			return null;
		}
		
		String outputS = Json.mapToString(response.toMap());
		//SLogger.d(TAG, "outputS=" + inputS);
		byte[] output = null;
		try {
			output = outputS.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			SLogger.exception(TAG, e);
		}
		
		return output; 
	}


	private ClientRequest handleRequest(ClientRequest request) {
		// TODO Auto-generated method stub
		ClientRequest response = null;
		
		Db db = new Db();
		if (!db.init(CServer.getInstance().redisHost)) {
			SLogger.e(TAG, "db not connected");
			response = new ClientRequest();
			response.status = ClientRequest.STATUS_ERROR_SERVER_ERROR;
			return response;
		}
		DbClient client = db.impersonate(request.clientId, request.hostId, request.authId);
		if (client == null) {
			SLogger.e(TAG, "db login failed");
			response = new ClientRequest();
			response.status = ClientRequest.STATUS_ERROR_AUTH_ERROR;
			return response;
		}
		
		switch (request.type) {
			case ClientRequest.TYPE_ECHO:
				response = handleEcho(db, client, request);
				break;
			case ClientRequest.TYPE_KEYBRD:
				response = handleKeyBrd(db, client, request);
				break;
			case ClientRequest.TYPE_SCREENSHOT:
				response = handleScreenshot(db, client, request);
				break;
			case ClientRequest.TYPE_USER_WINDOW:
				response = handleUserWindow(db, client, request);
				break;
			default:
				SLogger.e(TAG, "unsupported request type=" + request.type);
				response = new ClientRequest();
				response.status = ClientRequest.STATUS_ERROR_NOT_SUPPORTED;
				break;
		}
		
		return response;
	}


	private ClientRequest handleUserWindow(Db db, DbClient client, ClientRequest request) {
		// TODO Auto-generated method stub
		SLogger.d(TAG, "handleUserWindow data.length=" + request.data.length);

		DbResult result = db.handleUserWindow(client, request.systemTime, request.data);
		
		return new ClientRequest(result.error);
	}


	private ClientRequest handleScreenshot(Db db, DbClient client, ClientRequest request) {
		// TODO Auto-generated method stub
		SLogger.d(TAG, "handleScreenshot data.length=" + request.data.length);
		DbResult result = db.handleScreenshot(client, request.systemTime, request.data);

		return new ClientRequest(result.error);
	}


	private ClientRequest handleEcho(Db db, DbClient client, ClientRequest request) {
		// TODO Auto-generated method stub
		ClientRequest response = ClientRequest.clone(request);
		response.status = ClientRequest.STATUS_SUCCESS;
		
		return response;
	}
	
	private ClientRequest handleKeyBrd(Db db, DbClient client, ClientRequest request) {
		// TODO Auto-generated method stub
		ClientRequest response = new ClientRequest();
		response.status = ClientRequest.STATUS_SUCCESS;
		
		String events = null;
		try {
			events = new String(request.data, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			SLogger.exception(TAG, e);
		}
		
		if (events != null) {
			Map<String, String> map = Json.stringToMap(events);
			for (String key : map.keySet()) {
				Map<String, String> eventMap = Json.stringToMap(map.get(key));
				KeyBrdEvent event = new KeyBrdEvent();
				if (event.parseMap(eventMap))
					db.handleKeyBrd(client, event);
				
			}
		}
		
		return response;
	}
}
	

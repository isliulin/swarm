package cn.fx.desk.action;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONObject;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import cn.fx.desk.service.ApService;
import cn.fx.desk.service.DeskService;
import cn.fx.desk.service.SfService;
import cn.fx.desk.zoo.FxConstant;
import cn.fx.desk.zoo.FxProperties;
import cn.fx.desk.zoo.FxResult;
import cn.fx.desk.zoo.GeneralMethod;


/**
 * @author sunxy
 * @version 2014-7-18
 * @des 接入平台
 **/
@Controller
@RequestMapping("/ap")
public class AccessPlatform {
	@Autowired
	SfService sfService;
	@Autowired
	DeskService deskService;
	@Autowired
	ApService apService;
	/**
	 * redirecturl 是否可以不用配置
	 */
	@RequestMapping(value = "/sfcallback.do" ,method=RequestMethod.GET)
	public ModelAndView sfCallBack(HttpServletRequest request) {
		String code = request.getParameter("code");
		String state = request.getParameter("state");//移动端用户访问token
		System.out.println("======Salesforce 回调2======:"+state);
		ModelAndView mv = null;
		if(state!=null){
			try {
				int uid = Integer.parseInt(state);
				sfService.loginGetAccessToken(uid, code);
				mv = new ModelAndView("/cb_success");
			} catch (Exception e) {
				mv = new ModelAndView("/cb_fail");
				mv.addObject("errmsg", e.getMessage());
				
				e.printStackTrace();
			}
		}
		return mv;
	}
	/**
	 * Salesforce授权登陆，
	 * 移动平台header中传SESSION-TOKEN
	 * web传uid
	 */
	@RequestMapping(value = "/sflogin.do" ,method=RequestMethod.GET)
	public ModelAndView sfLogin(HttpServletRequest request) {
		String token = request.getParameter("SESSION-TOKEN");
		System.out.println("==========Salesforce 登陆22===============");
		int uid = 0;
		try {
			if(token != null){
				JSONObject jsonObj = GeneralMethod.getJsonFromUrl(FxProperties.USER_BY_TOKEN+token, "GET");
				int status = jsonObj.getInt("status");
				if(status == 0){
					JSONObject jsonUser = jsonObj.getJSONObject("result");
					uid = jsonUser.getInt("user_id");
				}
			}else if(request.getParameter("uid")!=null){
				uid = Integer.parseInt(request.getParameter("uid"));
			}else{
				//HttpSession session = request.getSession();
				
			}
			if(uid > 0){
				StringBuffer sb = new StringBuffer();
				sb.append("https://login.salesforce.com/services/oauth2/authorize?response_type=code&client_id=");
				sb.append(FxProperties.SF_CLIENT_ID);
				sb.append("&redirect_uri=");
				sb.append(URLEncoder.encode(FxProperties.SF_REDIRECT_URI, "UTF-8"));
				sb.append("&state=");
				sb.append(uid);
				ModelAndView mv = new ModelAndView("redirect:"+sb.toString());
				return mv;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@RequestMapping(value = "/deskcheck.do" ,method=RequestMethod.POST)
	public @ResponseBody Map deskCheck(HttpServletRequest request) {
		Map map = new HashMap();
		System.out.println("======移动端输入desk账号、密码======:"+request.getParameter("SESSION-TOKEN"));
		String token = request.getParameter("SESSION-TOKEN");//根据移动端token获取51desk应用的uid
		//根据token获取用户信息
		
		String deskUid = request.getParameter("desk_uid");
		String deskPwd = request.getParameter("desk_pwd");
		String deskSite = request.getParameter("desk_site");
		
		try {
			JSONObject jsonObj = GeneralMethod.getJsonFromUrl(FxProperties.USER_BY_TOKEN+token, "GET");
			int status = jsonObj.getInt("status");
			if(status == 0){
				JSONObject jsonUser = jsonObj.getJSONObject("result");
				int userId = jsonUser.getInt("user_id");
				
				//获取desk用户
				String cases = deskService.getStringFromDesk(deskUid,deskPwd, deskSite, FxConstant.DESK_API_ME);
				JSONObject mejson = JSONObject.fromObject(cases);
				String appUid = mejson.getString("id");
			    String appUserName = mejson.getString("email");//当前Email，是否取name
			    apService.addUserAppConfig(FxConstant.AP_DESK, userId, deskUid, deskPwd, deskSite, appUid, appUserName);
			    
			    return FxResult.TaskSuccessStatus();
			}
		} catch (Exception e) {
			map = FxResult.ReturnError(2001, e.getMessage());
			
			e.printStackTrace();
		}
		return map;
	}
	
	/********************Desk WEB输入账号密码登陆********************/
	@RequestMapping(value = "/deskjoin.do" ,method=RequestMethod.POST)
	public @ResponseBody Map deskJoin(HttpServletRequest request){
		Map map = new HashMap();
		System.out.println("************Desk WEB输入账号密码登陆*****************");
		//String token = request.getHeader("SESSION-TOKEN");
		//根据token获取用户信息
		int userId = Integer.parseInt(request.getParameter("uid"));
		
		String deskUid = request.getParameter("desk_uid");
		String deskPwd = request.getParameter("desk_pwd");
		String deskSite = request.getParameter("desk_site");
		
		//后面改成获取用户id的API
		String cases = deskService.getStringFromDesk(deskUid,deskPwd, deskSite, FxConstant.DESK_API_CASE);
		System.out.println("cases:"+cases);
		JSONObject casesjson = JSONObject.fromObject(cases);
		if(casesjson.containsKey("total_entries")){
			apService.addUserAppConfig(FxConstant.AP_DESK, userId, deskUid, deskPwd, deskSite, deskUid, deskUid);
			map.put("status", "0");
		}else{
			String message = "desk oauth failure";
			if(casesjson.containsKey("message")){
				message = casesjson.getString("message");
			}
			map = FxResult.ReturnError(2001, message);
		}
		return map;
	}
	
	
	/****************************Desk授权登陆************************************/
	/**
	 * @param SESSION-TOKEN,site
	 */
	@RequestMapping(value = "/desklogin.do" ,method=RequestMethod.GET)
	public ModelAndView deskLogin(HttpServletRequest request) throws Exception {
System.out.println("*********Desk授权登陆*************");
		String token = request.getParameter("SESSION-TOKEN");//根据移动端token获取51desk应用的uid
		ModelAndView mv = null;
		try {
			String site = request.getParameter("site");//用户输入的desk的site
			int uid = 0;
			if(token != null){
				JSONObject jsonObj = GeneralMethod.getJsonFromUrl(FxProperties.USER_BY_TOKEN+token, "GET");
				int status = jsonObj.getInt("status");
				if(status == 0){
					JSONObject jsonUser = jsonObj.getJSONObject("result");
					uid = jsonUser.getInt("user_id");//51desk用户id
					
				}
			}else if(request.getParameter("uid")!= null){
				uid = Integer.parseInt(request.getParameter("uid"));
			}else{
				//session
			}
			if(uid > 0){
				HttpSession session = request.getSession();
				OAuthConsumer consumer = new DefaultOAuthConsumer(
						FxProperties.DESK_TOKEN,FxProperties.DESK_SECRET);//,SignatureMethod.HMAC_SHA1
	
		        OAuthProvider provider = new DefaultOAuthProvider(
		                "https://"+site+".desk.com/oauth/request_token",
		                "https://"+site+".desk.com/oauth/access_token",
		                "https://"+site+".desk.com/oauth/authorize");
	
		        // we do not support callbacks, thus pass OOB
		        String authUrl = provider.retrieveRequestToken(consumer, FxProperties.CALLBACK_DESK);
				session.setAttribute("provider", provider);
				session.setAttribute("consumer", consumer);
				session.setAttribute("uid", uid);
				session.setAttribute("site", site);
				mv = new ModelAndView("redirect:"+authUrl);
				return mv;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	@RequestMapping(value = "/deskcb.do" ,method=RequestMethod.GET)
	public ModelAndView deskCallback(HttpServletRequest request) {
		HttpSession session = request.getSession();
		String oauth_verifier = request.getParameter("oauth_verifier");
System.out.println("**********Desk授权回调地址*************oauth_verifier:"+oauth_verifier);
		ModelAndView mv = null;
		try {
			int uid = Integer.parseInt(session.getAttribute("uid").toString());
			String instanceURL = session.getAttribute("site").toString();
			
			if(session.getAttribute("provider")!=null){
				OAuthProvider provider = (OAuthProvider) session.getAttribute("provider");
				OAuthConsumer consumer = (OAuthConsumer) session.getAttribute("consumer");
				
				provider.retrieveAccessToken(consumer, oauth_verifier);//根据临时token，换取正式的token
				String token = consumer.getToken();
				String secret = consumer.getTokenSecret();
				
				System.out.println("Access token: " + token);
			    System.out.println("Token secret: " + secret);
			    
			    //String me = deskService.getStringFromDesk(token, secret, instanceURL, FxConstant.DESK_API_ME);
			    String me = deskService.getDeskDataOAuth(token, secret, instanceURL, FxConstant.DESK_API_ME);
			    JSONObject mejson = JSONObject.fromObject(me);
			    String appUid = mejson.getString("id");
			    String appUserName = mejson.getString("email");//当前Email，是否取name
			    
			    apService.addUserAppConfig(FxConstant.AP_DESK, uid, token, secret, instanceURL, appUid, appUserName);
			    mv = new ModelAndView("/cb_success");
			}
		} catch (Exception e) {
			mv = new ModelAndView("/cb_fail");
			mv.addObject("errmsg", e.getMessage());
			e.printStackTrace();
		}
		return mv;
	}
}

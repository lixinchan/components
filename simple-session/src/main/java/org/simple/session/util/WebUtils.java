package org.simple.session.util;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * web util
 *
 * @author clx 2018/4/1.
 */
public final class WebUtils {
	private WebUtils() {
	}

	private static final Logger logger = LoggerFactory.getLogger(WebUtils.class);

	/**
	 * Headers about client's IP
	 */
	private static final String[] HEADERS_ABOUT_CLIENT_IP = {"X-Forwarded-For", "Proxy-Client-IP",
			"WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR", "HTTP_X_FORWARDED", "HTTP_X_CLUSTER_CLIENT_IP",
			"HTTP_CLIENT_IP", "HTTP_FORWARDED_FOR", "HTTP_FORWARDED", "HTTP_VIA", "REMOTE_ADDR"};

	/**
	 * get client ip
	 *
	 * @param request current request
	 * @return client ip
	 */
	public static String getClientIpAddr(HttpServletRequest request) {
		for (String header : HEADERS_ABOUT_CLIENT_IP) {
			String ip = request.getHeader(header);
			if (StringUtils.isNotBlank(ip) && StringUtils.equalsIgnoreCase("unknown", ip)) {
				return ip;
			}
		}
		return request.getRemoteAddr();
	}

	/**
	 * find cookie from request
	 *
	 * @param request current request
	 * @param name    cookie name
	 * @return cookie value or null
	 */
	public static Cookie findCookie(HttpServletRequest request, String name) {
		if (request != null) {
			Cookie[] cookies = request.getCookies();
			if (cookies != null && cookies.length > 0) {
				for (Cookie cookie : cookies) {
					if (cookie.getName().equals(name)) {
						return cookie;
					}
				}
			}
		}
		return null;
	}

	/**
	 * find cookie value
	 *
	 * @param request current request
	 * @param name    cookie name
	 * @return cookie value
	 */
	public static String findCookieValue(HttpServletRequest request, String name) {
		Cookie cookie = findCookie(request, name);
		return cookie != null ? cookie.getValue() : null;
	}

	/**
	 * add a cookie
	 */
	public static void addCookie(HttpServletRequest request, HttpServletResponse response, String name, String value,
								 int maxAge) {
		addCookie(request, response, name, value, null, maxAge, false);
	}

	/**
	 * add a cookie
	 *
	 * @param request
	 * @param response
	 * @param name
	 * @param value
	 * @param domain
	 * @param maxAge
	 * @param httpOnly
	 */
	public static void addCookie(HttpServletRequest request, HttpServletResponse response, String name, String value,
								 String domain, int maxAge, boolean httpOnly) {
		String contextPath = request.getContextPath();
		if (StringUtils.isBlank(contextPath)) {
			contextPath = "/";
		}
		addCookie(request, response, name, value, domain, contextPath, maxAge, httpOnly);
	}

	/**
	 * add a cookie
	 *
	 * @param request
	 * @param response
	 * @param name
	 * @param value
	 * @param domain
	 * @param contextPath
	 * @param maxAge
	 * @param httpOnly
	 */
	public static void addCookie(HttpServletRequest request, HttpServletResponse response, String name, String value,
								 String domain, String contextPath, int maxAge, boolean httpOnly) {
		if (request != null && response != null) {
			Cookie cookie = new Cookie(name, value);
			cookie.setMaxAge(maxAge);
			cookie.setSecure(request.isSecure());

			if (StringUtils.isBlank(contextPath)) {
				cookie.setPath("/");
			} else {
				cookie.setPath(contextPath);
			}

			if (StringUtils.isNotBlank(domain)) {
				cookie.setDomain(domain);
			}

			if (httpOnly) {
				cookie.setHttpOnly(true);
			}

			response.addCookie(cookie);
			logger.debug("Cookie update the sessionID.[name={},value={},maxAge={},httpOnly={},path={},domain={}]",
					cookie.getName(), cookie.getValue(), cookie.getMaxAge(), httpOnly, cookie.getPath(),
					cookie.getDomain());
		}
	}

	/**
	 * failure a cookie
	 *
	 * @param request
	 * @param response
	 * @param name
	 * @param domain
	 * @param contextPath
	 */
	public static void failureCookie(HttpServletRequest request, HttpServletResponse response, String name,
									 String domain, String contextPath) {
		if (request != null && response != null) {
			addCookie(request, response, name, null, domain, contextPath, 0, true);
		}
	}

	/**
	 * failure a cookie
	 *
	 * @param request
	 * @param response
	 * @param name
	 * @param domain
	 */
	public static void failureCookie(HttpServletRequest request, HttpServletResponse response, String name,
									 String domain) {
		String contextPath = request.getContextPath();
		if (StringUtils.isBlank(contextPath)) {
			contextPath = "/";
		}
		failureCookie(request, response, name, domain, contextPath);
	}

	/**
	 * failure a cookie
	 *
	 * @param request
	 * @param response
	 * @param name
	 */
	public static void failureCookie(HttpServletRequest request, HttpServletResponse response, String name) {
		failureCookie(request, response, name, null);
	}

	/**
	 * get request full url, include params
	 *
	 * @param request current request
	 * @return request full url, include params
	 */
	public static String getFullRequestUrl(HttpServletRequest request) {
		StringBuilder buff = new StringBuilder(request.getRequestURL().toString());
		String queryString = request.getQueryString();
		if (StringUtils.isNotBlank(queryString)) {
			buff.append("?").append(queryString);
		}
		return buff.toString();
	}

	/**
	 * redirect to url
	 *
	 * @param response
	 * @param url
	 * @param movePermanently true 301 for permanent redirect, false 302(temporary redirect)
	 * @throws IOException
	 */
	public static void redirect(HttpServletResponse response, String url, boolean movePermanently) throws IOException {
		if (!movePermanently) {
			response.sendRedirect(url);
		} else {
			response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
			response.setHeader("Location", url);
		}
	}

	/**
	 * browsers
	 */
	private static final String[] AGENT_INDEX = {"MSIE", "Firefox", "Chrome", "Opera", "Safari"};

	/**
	 * map regex for browsers
	 */
	private static final Map<String, Pattern> AGENT_PATTERNS = ImmutableMap.of(AGENT_INDEX[0],
			Pattern.compile("MSIE ([\\d.]+)"), AGENT_INDEX[1], Pattern.compile("Firefox/(\\d.+)"), AGENT_INDEX[2],
			Pattern.compile("Chrome/([\\d.]+)"), AGENT_INDEX[3], Pattern.compile("Opera[/\\s]([\\d.]+)"),
			AGENT_INDEX[4], Pattern.compile("Version/([\\d.]+)"));

	/**
	 * get user agent
	 *
	 * @param userAgent
	 * @return
	 */
	public static UserAgent getUserAgent(String userAgent) {
		if (StringUtils.isBlank(userAgent)) {
			return null;
		}

		for (String agentIdx : AGENT_INDEX) {
			Pattern pattern = AGENT_PATTERNS.get(agentIdx);
			Matcher matcher = pattern.matcher(userAgent);
			if (matcher.find()) {
				return new UserAgent(agentIdx, matcher.group(1));
			}
		}
		return null;
	}

	/**
	 * get user agent
	 *
	 * @param request
	 * @return
	 */
	public static UserAgent getUserAgent(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		String userAgentHead = request.getHeader("User-Agent");
		return getUserAgent(userAgentHead);
	}

	/**
	 * user agent class
	 */
	public static class UserAgent {

		private String name;
		private String version;

		/**
		 * @param name    agent name
		 * @param version agent version
		 */
		private UserAgent(String name, String version) {
			this.name = name;
			this.version = version;
		}

		public String getName() {
			return name;
		}

		public String getVersion() {
			return version;
		}
	}
}

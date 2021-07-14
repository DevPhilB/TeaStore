package utilities.rest.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.util.CharsetUtil;
import utilities.datamodel.SessionData;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;

public class CookieUtil {
    private final static ObjectMapper mapper = new ObjectMapper();

    /**
     * Decode cookie to session data
     *
     * @param cookieValue Cookie value as String
     * @return SessionData
     */
    public static SessionData decodeCookie(String cookieValue) {
        SessionData cookie = null;
        if (cookieValue != null) {
            try {
                cookie = mapper.readValue(
                        URLDecoder.decode(
                                cookieValue.substring("SessionData=".length()),
                                CharsetUtil.UTF_8
                        ),
                        SessionData.class
                );
            } catch(Exception e) {
                e.printStackTrace();
            }
        } else {
            cookie = new SessionData(
                    null,
                    null,
                    null,
                    null,
                    new ArrayList<>(),
                    null
            );
        }
        return cookie;
    }

    /**
     * Encode session data as cookie
     *
     * @param sessionData Session data
     * @return Cookie
     */
    public static Cookie encodeSessionData(SessionData sessionData, String gatewayHost) {
        try {
            String encodedCookie = URLEncoder.encode(
                    mapper.writeValueAsString(sessionData),
                    CharsetUtil.UTF_8
            );
            Cookie cookie = new DefaultCookie("SessionData", encodedCookie);
            cookie.setPath("/api");
            cookie.setDomain(gatewayHost);
            return cookie;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

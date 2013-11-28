/**
 * Copyright 2012 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.mongodb.*;
import org.joda.time.DateTime;
import play.Application;
import scala.Option;
import securesocial.core.*;
import securesocial.core.java.BaseUserService;
import securesocial.core.java.Token;

import java.net.UnknownHostException;
import java.util.*;

/**
 * A Sample Mongo user service in Java
 * <p/>
 * Note: This is NOT suitable for a production environment and is provided only as a guide.
 * A real implementation would persist things in a database
 */
public class MongoUserService extends BaseUserService {

    private final MongoClient mongoClient;
    private final DB db;
    private final DBCollection usersCollection;
    private final DBCollection tokensCollection;
    //    private HashMap<String, Identity> users  = new HashMap<String, Identity>();
//    private HashMap<String, Token> tokens = new HashMap<String, Token>();
    private final ObjectMapper objectMapper;

    public static void main(String[] args) {

    }


    public MongoUserService(Application application) {
        super(application);

        try {
            mongoClient = new MongoClient("vm01");
            db = mongoClient.getDB("ss_demo");
            usersCollection = db.getCollection("users");
            tokensCollection = db.getCollection("tokens");
            objectMapper = new ObjectMapper();
        } catch (UnknownHostException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public Identity doSave(Identity user) {
//        users.put(user.identityId().userId() + user.identityId().providerId(), user);

//        Map<String,Object> userMap = objectMapper.convertValue(user, Map.class);
        Map<String, Object> userMap = userToMap(user);

        usersCollection.insert(new BasicDBObject(userMap));

        System.out.printf("inserted: %s%n", userMap);

        // this sample returns the same user object, but you could return an instance of your own class
        // here as long as it implements the Identity interface. This will allow you to use your own class in the
        // protected actions and event callbacks. The same goes for the doFind(UserId userId) method.
        return user;
    }

    public static Map<String, Object> userToMap(Identity user) {
        Map<String, Object> userMap = new HashMap<>();

        Option<OAuth1Info> oAuth1InfoOption = user.oAuth1Info();

        if (oAuth1InfoOption.isDefined()) {
            OAuth1Info oAuth1Info = oAuth1InfoOption.get();
            HashMap<String, Object> auth1Map = new HashMap<String, Object>();

            auth1Map.put("secret", oAuth1Info.secret());
            auth1Map.put("token", oAuth1Info.token());
            auth1Map.put("productPrefix", oAuth1Info.productPrefix());

            userMap.put("oAuth1Info", auth1Map);
        }

        Option<OAuth2Info> oAuth2InfoOption = user.oAuth2Info();

        if (oAuth2InfoOption.isDefined()) {
            OAuth2Info oAuth2Info = oAuth2InfoOption.get();
            HashMap<String, Object> auth2Map = new HashMap<String, Object>();

            auth2Map.put("accessToken", oAuth2Info.accessToken());
            auth2Map.put("productPrefix", oAuth2Info.productPrefix());

            putOption(auth2Map, "refreshToken", oAuth2Info.refreshToken());
            putOption(auth2Map, "expiresIn", oAuth2Info.expiresIn());
            putOption(auth2Map, "tokenType", oAuth2Info.tokenType());

            userMap.put("oAuth2Info", auth2Map);
        }

        userMap.put("fullName", user.fullName());
        userMap.put("userId", user.identityId().userId());
        userMap.put("providerId", user.identityId().providerId());

        putOption(userMap, "avatarUrl", user.avatarUrl());

        putOption(userMap, "email", user.email());
//        userMap.put("identityId", user.identityId());

        return userMap;
    }

    @Override
    public Identity doFind(IdentityId userId) {

        Map<String, Object> userMap = usersCollection.findOne(new BasicDBObject()
                .append("userId", userId.userId())
                .append("providerId", userId.providerId())
        ).toMap();

        return mapToIdentity(userId, userMap);
    }

    public static Identity mapToIdentity(IdentityId userId, Map<String, Object> userMap) {
        AuthenticationMethod authenticationMethod;

        Option<OAuth1Info> oAuth1Info = Option.empty();
        Option<OAuth2Info> oAuth2Info = Option.empty();

        if (userMap.containsKey("oAuth1Info")) {
            authenticationMethod = AuthenticationMethod.OAuth1();

            Map<String, Object> auth1Map = (Map<String, Object>) userMap.get("oAuth1Info");

            oAuth1Info = Option.apply(new OAuth1Info((String) auth1Map.get("token"), (String) auth1Map.get("secret")));
        } else {
            authenticationMethod = AuthenticationMethod.OAuth2();

            Map<String, Object> auth2Map = (Map<String, Object>) userMap.get("oAuth2Info");

            oAuth2Info = Option.apply(
                    new OAuth2Info(
                            (String) auth2Map.get("accessToken"),
                            getOption(auth2Map, "tokenType", String.class),
                            Option.empty(),
                            getOption(auth2Map, "refreshToken", String.class)
                    ));
        }

        return new SocialUser(
                userId, null, null, (String) userMap.get("fullName"),
                Option.<String>empty(),
                getOption(userMap, "avatarUrl", String.class),
                authenticationMethod,
                oAuth1Info,
                oAuth2Info,
                Option.<PasswordInfo>empty()
        );
    }


    private static void putOption(Map<String, Object> userMap, String field, Option<?> option) {
        if (option.isDefined()) {
            userMap.put(field, option.get());
        }
    }

    private static <T> Option<T> getOption(Map<String, Object> userMap, String field, Class<T> tClass) {
        if (!userMap.containsKey(field)) {
            return Option.empty();
        }

        Object o = userMap.get(field);

        Option<T> apply = (Option<T>) Option.apply(o);
        return apply;
    }

    @Override
    public void doSave(Token token) {
        Map<String, Object> map = tokenToMap(token);

        tokensCollection.save(new BasicDBObject(map));
    }

    public static Map<String, Object> tokenToMap(Token token) {
        Map<String, Object> map = new HashMap();

        map.put("email", token.email);
        map.put("uuid", token.uuid);
        map.put("creationTime", token.creationTime);
        map.put("expirationTime", token.expirationTime);
        map.put("isSignUp", token.isSignUp);

        return map;
    }

    @Override
    public Token doFindToken(String tokenId) {
        DBObject tokenObj = tokensCollection.findOne(new BasicDBObject("tokenId", tokenId));

        return dbObjToToken(tokenObj);
    }

    private static Token dbObjToToken(DBObject tokenObj) {
        Token token = new Token();

        token.email = (String) tokenObj.get("email");
        token.uuid = (String) tokenObj.get("uuid");
        token.isSignUp = (Boolean) tokenObj.get("isSignUp");
        token.creationTime = (DateTime) tokenObj.get("creationTime");
        token.expirationTime = (DateTime) tokenObj.get("expirationTime");

        return token;
    }

    @Override
    public Identity doFindByEmailAndProvider(String email, String providerId) {
        Identity result = null;

        usersCollection.findOne(new BasicDBObject()
                .append("email", email)
                .append("providerId", providerId));

        return result;
    }

    @Override
    public void doDeleteToken(String uuid) {
        tokensCollection.remove(new BasicDBObject("uuid", uuid));
    }

    @Override
    public void doDeleteExpiredTokens() {
        List<String> expiredGuids = new ArrayList<>();


        for (DBObject dbObject : tokensCollection.find()) {
            Token token = dbObjToToken(dbObject);

            if (token.isExpired()) {
                expiredGuids.add((String) dbObject.get("_id"));
            }
        }

        for (String _id : expiredGuids) {
            tokensCollection.remove(new BasicDBObject("_id", _id));
        }
    }
}

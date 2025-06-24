package pro.eng.yui.oss.d2h.botIF;

import pro.eng.yui.oss.d2h.consts.StringConsts;
import pro.eng.yui.oss.d2h.db.field.*;

public class ResponseToken {
    
    private AccessToken accessToken;
    public void setAccessToken(String accessToken) {
        setAccessToken(new AccessToken(accessToken));
    }
    public void setAccessToken(AccessToken accessToken){
        this.accessToken = accessToken;
    }
    public AccessToken getAccessToken() {
        return accessToken;
    }

    private RefreshToken refreshToken;
    public void setRefreshToken(String refreshToken) {
        setRefreshToken(new RefreshToken(refreshToken));
    }
    public void setRefreshToken(RefreshToken refreshToken){
        this.refreshToken = refreshToken;
    }
    public RefreshToken getRefreshToken() {
        return refreshToken;
    }
    
    private TokenType tokenType;
    public void setTokenType(String tokenType) {
        setTokenType(new TokenType(tokenType));
    }
    public void setTokenType(TokenType tokenType){
        this.tokenType = tokenType;
    }
    public TokenType getTokenType() {
        return tokenType;
    }
    
    private ExpireAt expiresAt;
    public void setExpiresAt(long expireIn){
        setExpiresAt(new ExpireAt(expireIn));
    }
    public void setExpiresAt(IssuedAt issuedAt, long expireIn){
        setExpiresAt(new ExpireAt(issuedAt, expireIn));
    }
    public void setExpiresAt(ExpireAt expiresAt){
        this.expiresAt = expiresAt;
    }
    public ExpireAt getExpiresAt(){
        return expiresAt;
    }
   
    private Scope scope;
    public void setScope(String scope) {
        setScope(new Scope(scope));
    }
    public void setScope(Scope scope){
        this.scope = scope;
    }
    public Scope getScope() {
        return scope;
    }

    @Override
    public String toString() {
        return StringConsts.gson.toJson(this);
    }
}

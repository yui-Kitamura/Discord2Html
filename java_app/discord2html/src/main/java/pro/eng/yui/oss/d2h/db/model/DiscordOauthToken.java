package pro.eng.yui.oss.d2h.db.model;

import pro.eng.yui.oss.d2h.botIF.ResponseToken;
import pro.eng.yui.oss.d2h.db.field.*;

public class DiscordOauthToken {
    
    private SeqId seq_id;
    public void setSeqId(SeqId newValue){
        this.seq_id = newValue;
    }
    public SeqId getSeqId(){
        return seq_id;
    }    
    
    private UserId user_id;
    public void setUserId(UserId newValue) {
        this.user_id = newValue;
    }
    public UserId getUserId() {
        return user_id;
    }

    private AccessToken access_token;
    public void setAccessToken(AccessToken newValue) {
        this.access_token = newValue;
    }
    public AccessToken getAccessToken() {
        return access_token;
    }

    private RefreshToken refresh_token;
    public void setRefreshToken(RefreshToken newValue) {
        this.refresh_token = newValue;
    }
    public RefreshToken getRefreshToken() {
        return refresh_token;
    }

    private TokenType token_type;
    public void setTokenType(TokenType newValue) {
        this.token_type = newValue;
    }
    public TokenType getTokenType() {
        return token_type;
    }

    private Scope scope;
    public void setScope(Scope newValue) {
        this.scope = newValue;
    }
    public Scope getScope() {
        return scope;
    }

    private ExpireAt expires_at;
    public void setExpireAt(ExpireAt newValue) {
        this.expires_at = newValue;
    }
    public ExpireAt getExpireAt() {
        return expires_at;
    }

    private IssuedAt issued_at;
    public void setIssuedAt(IssuedAt newValue) {
        this.issued_at = newValue;
    }
    public IssuedAt getIssuedAt() {
        return issued_at;
    }

    private CreatedAt created_at;
    public void setCreatedAt(CreatedAt newValue) {
        this.created_at = newValue;
    }
    public CreatedAt getCreatedAt() {
        return created_at;
    }

    private UpdatedAt updated_at;
    public void setUpdatedAt(UpdatedAt newValue) {
        this.updated_at = newValue;
    }
    public UpdatedAt getUpdatedAt() {
        return updated_at;
    }
    
    public DiscordOauthToken(){ /* nothing to do */ }
    public DiscordOauthToken(UserId userId, ResponseToken apiRes){
        super();
        setUserId(userId);
        setAccessToken(apiRes.getAccessToken());
        setTokenType(apiRes.getTokenType());
        setRefreshToken(apiRes.getRefreshToken());
        setScope(apiRes.getScope());
        setExpireAt(apiRes.getExpiresAt());
    }

}

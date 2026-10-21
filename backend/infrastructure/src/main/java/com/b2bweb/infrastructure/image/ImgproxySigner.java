package com.b2bweb.infrastructure.image;
import com.b2bweb.api.common.Secret;
import com.b2bweb.api.port.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
/** Public-source-only imgproxy HMAC calculation. No network or provider SDK. */
public final class ImgproxySigner implements ImageUrlSigner {
 final URI base;final byte[] key,salt;
 public ImgproxySigner(URI base,Secret hexKey,Secret hexSalt){
  if(!Set.of("http","https").contains(base.getScheme()) || base.getHost()==null || base.getRawUserInfo()!=null || base.getQuery()!=null || base.getFragment()!=null)throw new IllegalArgumentException("图片代理地址无效");
  this.base=base;key=HexFormat.of().parseHex(hexKey.value());salt=HexFormat.of().parseHex(hexSalt.value());if(key.length<32 || salt.length<16)throw new IllegalArgumentException("图片签名密钥长度不足");
 }
 public URI sign(ImageTransform t){
  if(t.height()!=null && (t.height()<1 || t.height()>4096) || t.focalX()!=null && (!Double.isFinite(t.focalX()) || t.focalX()<0 || t.focalX()>1) || t.focalY()!=null && (!Double.isFinite(t.focalY()) || t.focalY()<0 || t.focalY()>1) || !t.source().value().matches("[1-9][0-9]*/.*"))throw new IllegalArgumentException("图片转换参数或公开媒体键无效");
  String format=t.format().name().equals("JPEG")?"jpg":t.format().name().toLowerCase(Locale.ROOT);
  int quality=t.quality()!=null?t.quality():format.equals("avif")?60:format.equals("webp")?75:80;
  String path="/rs:"+t.mode().name().toLowerCase(Locale.ROOT)+":"+t.width()+":"+(t.height()==null?0:t.height())+"/g:fp:"+(t.focalX()==null?.5:t.focalX())+":"+(t.focalY()==null?.5:t.focalY())+"/q:"+quality+"/plain/"+t.source().value()+"@"+format;
  try{var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));mac.update(salt);String signature=Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(path.getBytes(StandardCharsets.UTF_8)));return URI.create(base.toString().replaceAll("/+$","")+"/"+signature+path);}
  catch(java.security.GeneralSecurityException e){throw new IllegalStateException("图片签名算法不可用",e);}
 }
}

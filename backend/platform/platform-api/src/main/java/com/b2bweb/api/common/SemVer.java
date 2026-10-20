// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.common;
import java.util.*;
import java.time.*;
import java.net.*;
import java.io.*;
import java.util.function.*;
import java.lang.annotation.*;
import org.jspecify.annotations.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import tools.jackson.databind.*;
import tools.jackson.databind.node.*;
import com.b2bweb.api.common.*;
import com.b2bweb.api.site.*;
import com.b2bweb.api.security.*;
import com.b2bweb.api.content.*;
import com.b2bweb.api.url.*;
import com.b2bweb.api.media.*;
import com.b2bweb.api.taxonomy.*;
import com.b2bweb.api.form.*;
import com.b2bweb.api.notification.*;
import com.b2bweb.api.translation.*;
import com.b2bweb.api.seo.*;
import com.b2bweb.api.delivery.*;
import com.b2bweb.api.extension.*;
import com.b2bweb.api.event.*;
import com.b2bweb.api.port.*;

/** 语义化版本（模块版本、平台版本）。 */
public record SemVer(int major, int minor, int patch,
                     @org.jspecify.annotations.Nullable String preRelease) implements Comparable<SemVer> {

    /** 解析 "1.2.3" 或 "1.2.3-rc.1"；格式错误抛出 IllegalArgumentException。 */
    public static SemVer parse(String text) {
        var match = java.util.regex.Pattern.compile("^(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$").matcher(text);
        if (!match.matches()) throw new IllegalArgumentException("非法语义版本: " + text);
        String pre = match.group(4);
        if (pre != null) for (String id : pre.split("\\."))
            if (id.matches("[0-9]+") && id.length() > 1 && id.startsWith("0")) throw new IllegalArgumentException("版本数字不得有前导零");
        return new SemVer(Integer.parseInt(match.group(1)), Integer.parseInt(match.group(2)), Integer.parseInt(match.group(3)), pre);
    }

    @Override
    public int compareTo(SemVer o) {
        int result = Integer.compare(major, o.major); if (result != 0) return result;
        result = Integer.compare(minor, o.minor); if (result != 0) return result;
        result = Integer.compare(patch, o.patch); if (result != 0) return result;
        if (preRelease == null) return o.preRelease == null ? 0 : 1;
        if (o.preRelease == null) return -1;
        String[] left = preRelease.split("\\."), right = o.preRelease.split("\\.");
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            boolean a = left[i].matches("[0-9]+"), b = right[i].matches("[0-9]+");
            result = a && b ? new java.math.BigInteger(left[i]).compareTo(new java.math.BigInteger(right[i]))
                    : a != b ? (a ? -1 : 1) : left[i].compareTo(right[i]);
            if (result != 0) return result;
        }
        return Integer.compare(left.length, right.length);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch + (preRelease == null ? "" : "-" + preRelease);
    }
}

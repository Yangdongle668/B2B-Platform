// Generated from platform-framework.md chapter 4; edit the canonical document.
package com.b2bweb.api.form;
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

public interface FormApi {

    /** 按 key 查找表单（含停用）。 */
    Optional<FormSummary> findForm(String formKey);

    Optional<FormSummary> findForm(long formId);

    /** 某用途的启用表单（product 的留资表单选择器：purpose = DOWNLOAD）。 */
    List<FormSummary> forms(FormPurpose purpose);

    /** 提交记录（含 REJECT；REJECT 在保留期内仍可读取）。 */
    Optional<SubmissionView> findSubmission(long submissionId);
}

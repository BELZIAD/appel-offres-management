package com.aomanagement.service;

import com.aomanagement.entity.CompanySettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {
    private final CompanySettingsService companySettingsService;

    public String render(String title, String body) {
        CompanySettings s = companySettingsService.entity();
        return """
                <div style="font-family:Arial,sans-serif;background:#f6f8fb;padding:24px;color:#172033">
                  <div style="max-width:680px;margin:auto;background:#fff;border:1px solid #e5e7eb;border-radius:8px;overflow:hidden">
                    <div style="padding:20px;background:%s;color:#fff">
                      <div style="font-size:18px;font-weight:700">%s</div>
                      <div style="font-size:13px;opacity:.9">%s</div>
                    </div>
                    <div style="padding:24px">
                      <h2 style="margin-top:0">%s</h2>
                      <div style="font-size:14px;line-height:1.6">%s</div>
                    </div>
                    <div style="padding:16px 24px;background:#f8fafc;font-size:12px;color:#64748b">
                      %s<br/>%s%s%s
                    </div>
                  </div>
                </div>
                """.formatted(
                safe(s.getPrimaryColor()),
                safe(s.getCompanyName()),
                safe(s.getApplicationName()),
                safe(title),
                safe(body).replace("\n", "<br/>"),
                safe(s.getCompanyName()),
                s.getAddress() == null ? "" : safe(s.getAddress()) + "<br/>",
                s.getPhone() == null ? "" : "Tel: " + safe(s.getPhone()) + " ",
                s.getEmail() == null ? "" : "Email: " + safe(s.getEmail())
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

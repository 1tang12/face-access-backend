from PIL import Image, ImageDraw, ImageFont
import math


W, H = 1600, 1200
OUT = "/Users/drifter/Desktop/毕业设计/zbmj/database/face_access_system_uml_fk_compact.png"


FONT_PATHS = [
    "/System/Library/Fonts/PingFang.ttc",
    "/System/Library/Fonts/STHeiti Light.ttc",
    "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
]


def get_font(size):
    for path in FONT_PATHS:
        try:
            return ImageFont.truetype(path, size)
        except Exception:
            continue
    return ImageFont.load_default()


font_title = get_font(34)
font_sub = get_font(18)
font_head = get_font(18)
font_body = get_font(16)
font_small = get_font(14)


img = Image.new("RGB", (W, H), "white")
d = ImageDraw.Draw(img)


def text_center(y, text, font, fill):
    bbox = d.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    d.text(((W - tw) / 2, y), text, font=font, fill=fill)


text_center(18, "人脸识别门禁系统数据库 UML 图", font_title, "#111111")
text_center(64, "显式外键约束版", font_sub, "#666666")


boxes = {
    "sys_user": {"xy": (70, 130, 390, 300), "lines": ["sys_user", "PK id", "username", "real_name", "phone", "status"]},
    "sys_user_role": {"xy": (70, 400, 390, 540), "lines": ["sys_user_role", "PK id", "FK user_id", "FK role_id"]},
    "sys_role": {"xy": (640, 130, 960, 270), "lines": ["sys_role", "PK id", "role_name", "role_code"]},
    "sys_role_permission": {"xy": (640, 400, 960, 540), "lines": ["sys_role_permission", "PK id", "FK role_id", "FK permission_id"]},
    "sys_permission": {"xy": (1170, 130, 1490, 300), "lines": ["sys_permission", "PK id", "permission_name", "permission_code", "FK parent_id"]},
    "face_feature": {"xy": (40, 660, 350, 840), "lines": ["face_feature", "PK id", "FK user_id", "face_image_path", "quality_score"]},
    "access_record": {"xy": (430, 660, 760, 860), "lines": ["access_record", "PK id", "FK user_id", "FK device_id", "access_time", "result"]},
    "access_device": {"xy": (830, 660, 1140, 860), "lines": ["access_device", "PK id", "device_name", "device_code", "location", "status"]},
    "attendance_record": {"xy": (1200, 660, 1520, 840), "lines": ["attendance_record", "PK id", "FK user_id", "attendance_date", "status"]},
    "visitor_appointment": {"xy": (260, 980, 690, 1170), "lines": ["visitor_appointment", "PK id", "FK visitor_user_id", "FK reviewer_id", "status", "valid_start_time", "valid_end_time"]},
    "operation_log": {"xy": (930, 980, 1330, 1130), "lines": ["operation_log", "PK id", "FK user_id", "operation_type", "operate_time"]},
}


border = "#2f3b52"
header_fill = "#eaf1fb"
body_fill = "#ffffff"
line_fill = "#5b6b88"


def draw_box(name, info):
    x1, y1, x2, y2 = info["xy"]
    d.rounded_rectangle((x1, y1, x2, y2), radius=18, outline=border, width=3, fill=body_fill)
    d.rounded_rectangle((x1, y1, x2, y1 + 40), radius=18, outline=border, width=3, fill=header_fill)
    d.rectangle((x1, y1 + 18, x2, y1 + 40), fill=header_fill)
    d.line((x1, y1 + 40, x2, y1 + 40), fill=border, width=2)

    title = info["lines"][0]
    bbox = d.textbbox((0, 0), title, font=font_head)
    tw = bbox[2] - bbox[0]
    d.text((x1 + (x2 - x1 - tw) / 2, y1 + 9), title, font=font_head, fill="#182233")

    y = y1 + 54
    for line in info["lines"][1:]:
        d.text((x1 + 16, y), line, font=font_body, fill="#263248")
        y += 24


for name, info in boxes.items():
    draw_box(name, info)


def ctop(name):
    x1, y1, x2, y2 = boxes[name]["xy"]
    return ((x1 + x2) // 2, y1)


def cbot(name):
    x1, y1, x2, y2 = boxes[name]["xy"]
    return ((x1 + x2) // 2, y2)


def cleft(name):
    x1, y1, x2, y2 = boxes[name]["xy"]
    return (x1, (y1 + y2) // 2)


def cright(name):
    x1, y1, x2, y2 = boxes[name]["xy"]
    return (x2, (y1 + y2) // 2)


def draw_arrow(points, label=None, label_xy=None, color=line_fill, width=3):
    for i in range(len(points) - 1):
        d.line((points[i], points[i + 1]), fill=color, width=width)

    (x1, y1), (x2, y2) = points[-2], points[-1]
    ang = math.atan2(y2 - y1, x2 - x1)
    size = 11
    a1 = ang + math.pi / 7
    a2 = ang - math.pi / 7
    p1 = (x2 - size * math.cos(a1), y2 - size * math.sin(a1))
    p2 = (x2 - size * math.cos(a2), y2 - size * math.sin(a2))
    d.polygon([points[-1], p1, p2], fill=color)

    if label:
        if label_xy is None:
            lx = (points[0][0] + points[-1][0]) / 2
            ly = (points[0][1] + points[-1][1]) / 2
        else:
            lx, ly = label_xy
        bbox = d.textbbox((0, 0), label, font=font_small)
        tw = bbox[2] - bbox[0]
        th = bbox[3] - bbox[1]
        d.rounded_rectangle((lx - tw / 2 - 4, ly - th / 2 - 2, lx + tw / 2 + 4, ly + th / 2 + 2), radius=5, fill="white")
        d.text((lx - tw / 2, ly - th / 2), label, font=font_small, fill="#42526e")


# 1. user -> user_role (straight, no crossing)
draw_arrow([cbot("sys_user"), ctop("sys_user_role")], "FK user_id", (315, 350))

# 2. role -> user_role (route above middle row)
draw_arrow([cbot("sys_role"), (800, 340), (230, 340), ctop("sys_user_role")], "FK role_id", (500, 325))

# 3. role -> role_permission (straight)
draw_arrow([cbot("sys_role"), ctop("sys_role_permission")], "FK role_id", (860, 350))

# 4. permission -> role_permission
draw_arrow([cbot("sys_permission"), (1330, 350), (800, 350), ctop("sys_role_permission")], "FK permission_id", (1130, 327))

# 5. permission -> permission (self parent)
draw_arrow([(1300, 130), (1300, 110), (1110, 110), (1110, 195), (1170, 195)], "FK parent_id", (1290, 96))

# 6. user -> face_feature (left corridor)
draw_arrow([cleft("sys_user"), (25, 215), (25, 620), (195, 620), ctop("face_feature")], "FK user_id", (120, 602))

# 7. user -> access_record (middle corridor)
draw_arrow([cright("sys_user"), (410, 215), (410, 620), (595, 620), ctop("access_record")], "FK user_id", (520, 602))

# 8. user -> attendance_record (top corridor over third row)
draw_arrow([cright("sys_user"), (430, 230), (430, 610), (1360, 610), ctop("attendance_record")], "FK user_id", (1260, 592))

# 9. access_device -> access_record (horizontal gap)
draw_arrow([cleft("access_device"), (790, 760), cright("access_record")], "FK device_id", (790, 740))

# 10. user -> visitor_appointment (visitor_user_id)
draw_arrow([cright("sys_user"), (405, 245), (405, 940), (430, 940), ctop("visitor_appointment")], "FK visitor_user_id", (360, 920))

# 11. user -> visitor_appointment (reviewer_id)
draw_arrow([cright("sys_user"), (420, 260), (420, 915), (520, 915), (520, 980)], "FK reviewer_id", (465, 895))

# 12. user -> operation_log
draw_arrow([cright("sys_user"), (425, 275), (425, 930), (1130, 930), ctop("operation_log")], "FK user_id", (915, 912))


img.save(OUT)
print(OUT)

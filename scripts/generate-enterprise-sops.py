#!/usr/bin/env python3
"""Generate the 15 controlled Renix MES work-instruction PDFs."""

from __future__ import annotations

import argparse
import shutil
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    KeepTogether,
    PageBreak,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)


COMPANY = "雷尼克斯体育用品制造有限公司"
VERSION = "V1.0"
EFFECTIVE_DATE = "2026-06-30"
FONT_PATH = Path(r"C:\Windows\Fonts\simhei.ttf")

NAVY = colors.HexColor("#18364A")
TEAL = colors.HexColor("#167A78")
PALE_TEAL = colors.HexColor("#E8F3F1")
PALE_BLUE = colors.HexColor("#EDF3F7")
INK = colors.HexColor("#24343D")
MUTED = colors.HexColor("#60727C")
LINE = colors.HexColor("#B9C8CF")
ALERT = colors.HexColor("#9E3F32")
WHITE = colors.white


def sop_records() -> list[dict]:
    common_records = ["MES生产任务/工单号", "原料或在制批次号", "操作员与复核人", "实测参数及异常记录"]
    return [
        {
            "filename": "sop-feather-sort-duck-v1.0.pdf",
            "code": "LNX-SOP-01",
            "title": "鸭毛片分选作业指导书",
            "operation": "OP-FEATHER-SORT / 毛片分选",
            "scope": "适用于雷尼克斯 T300 训练级系列鸭毛片的来料复核、配组和上线前分选。",
            "ppe": "佩戴防尘口罩和指套；开启局部除尘，禁止在分选台饮食。",
            "tools": "标准光源台、毫米卡尺、电子秤、16槽配组盘、合格/待判/不合格周转盒。",
            "parameters": [
                ("毛片长度", "67 mm", "64-70 mm", "逐批抽样测量"),
                ("外观", "无断毛、霉变、油污", "不得混入明显色差", "标准光源下目检"),
                ("单球配组", "16 根", "同组弯度、厚薄协调", "配组盘复核"),
            ],
            "steps": [
                "核对供应商、物料编码、来料批次和待检状态，未放行批次不得上线。",
                "在标准光源下剔除断梗、霉变、油污、虫蛀和明显色差毛片，并分别计数。",
                "测量毛片长度，按 64-70 mm 标准筛选；超差品放入不合格盒并保留批次标识。",
                "按弯度、厚薄和左右羽配成 16 根一组，组内毛片不得跨原料批次混放。",
                "复核数量和外观后打印在制标签，记录原料批次并移交球头成型/植毛工位。",
            ],
            "quality": ["批次、标签与实物一致", "组内 16 根齐套且外观协调", "不合格品隔离并记录数量"],
            "abnormal": "发现霉变、批量色差或尺寸超差趋势时立即暂停分选，隔离整批并通知质量工程师复判。",
            "records": common_records,
        },
        {
            "filename": "sop-feather-sort-goose-v1.0.pdf",
            "code": "LNX-SOP-02",
            "title": "鹅毛片分选作业指导书",
            "operation": "OP-FEATHER-SORT / 毛片分选",
            "scope": "适用于雷尼克斯 M700 比赛级系列鹅毛片的来料复核、等级筛选和 16 根配组。",
            "ppe": "佩戴防尘口罩和指套；开启局部除尘，搬运原料箱时注意防割伤。",
            "tools": "标准光源台、毫米卡尺、电子秤、弯度样板、16槽配组盘、隔离周转盒。",
            "parameters": [
                ("毛片长度", "67 mm", "64-70 mm", "逐批抽样测量"),
                ("外观", "完整、洁净、无霉变", "无明显色差和损伤", "标准光源下目检"),
                ("比赛级配组", "16 根", "弯度、厚薄、羽面一致", "弯度样板复核"),
            ],
            "steps": [
                "核对鹅毛片物料编码、供应商批次和质量放行状态，扫描批次标签后开始作业。",
                "在标准光源下逐束检查羽面、羽梗、色泽和清洁度，缺陷品单独隔离。",
                "测量长度并按 64-70 mm 判定，不允许通过修剪掩盖来料尺寸不合格。",
                "使用弯度样板完成左右羽、弯度和厚薄配组，每组固定 16 根并保持同批次。",
                "班组长首件复核后打印在制标签，记录合格数、不良数和移交数量。",
            ],
            "quality": ["比赛级毛片不得混入鸭毛片", "配组一致性满足首件样板", "批次正向与反向追溯可用"],
            "abnormal": "发现材质混料、批量断梗或弯度明显漂移时停止使用该批次，触发来料质量复检。",
            "records": common_records,
        },
        {
            "filename": "sop-cork-form-composite-v1.0.pdf",
            "code": "LNX-SOP-03",
            "title": "复合软木球头成型作业指导书",
            "operation": "OP-CORK-FORM / 球头成型",
            "scope": "适用于 T300 系列复合软木球头的领料、成型、修整和首件确认。",
            "ppe": "佩戴护目镜和防尘口罩；设备防护罩未闭合时禁止启动。",
            "tools": "球头成型机、直径量规、毫米卡尺、电子秤、除尘装置、合格样件。",
            "parameters": [
                ("球头直径", "26.5 mm", "25-28 mm", "首件及巡检测量"),
                ("外观", "表面平整", "无裂纹、缺口、偏心", "目检并滚动复核"),
                ("批次隔离", "一批一容器", "不得跨批混装", "标签核对"),
            ],
            "steps": [
                "按工单扫描复合软木批次，核对可用状态和领料数量。",
                "清洁模具和夹具，安装防护罩，空载点动确认设备无异常。",
                "加工首件并测量直径、圆度和外观，首件确认前不得批量生产。",
                "按规定频次巡检，及时清除碎屑；发现刀具磨损趋势时暂停调整。",
                "合格球头按批次装盒并贴在制标签，不合格品进入锁定容器。",
            ],
            "quality": ["直径处于 25-28 mm", "球头无裂纹、缺口和明显偏心", "首件与巡检结果完整"],
            "abnormal": "连续出现尺寸漂移、裂纹或设备异响时立即停机，保留最近一次合格样件并通知班组长。",
            "records": common_records,
        },
        {
            "filename": "sop-cork-form-natural-v1.0.pdf",
            "code": "LNX-SOP-04",
            "title": "天然软木球头成型作业指导书",
            "operation": "OP-CORK-FORM / 球头成型",
            "scope": "适用于 M700 比赛级系列天然软木球头的配重、成型和首件确认。",
            "ppe": "佩戴护目镜和防尘口罩；确认设备急停、防护罩和除尘装置有效。",
            "tools": "球头成型机、直径量规、毫米卡尺、0.01 g 电子秤、合格样件。",
            "parameters": [
                ("球头直径", "26.5 mm", "25-28 mm", "首件及巡检测量"),
                ("材质外观", "纹理均匀", "无裂纹、孔洞、分层", "标准光源下目检"),
                ("配重原则", "同工单一致", "按产品工艺卡执行", "电子秤复核"),
            ],
            "steps": [
                "扫描天然软木批次，核对检验放行状态、工单产品和领用数量。",
                "按工艺卡完成软木坯配重分组，未经复核不得混入不同重量组。",
                "清洁模具后加工首件，检查直径、纹理、裂纹和偏心。",
                "首件由班组长确认；批量加工期间按频次复测并记录刀具状态。",
                "合格品按重量组和原料批次分别装盒，移交植毛工序并完成扫描。",
            ],
            "quality": ["天然软木材质和批次正确", "直径处于 25-28 mm", "重量分组、在制标签和数量一致"],
            "abnormal": "发现批量孔洞、分层、配重偏移或尺寸趋势异常时暂停该批次，提交质量复判。",
            "records": common_records,
        },
        {
            "filename": "sop-feather-insert-v1.0.pdf",
            "code": "LNX-SOP-05",
            "title": "植毛作业指导书",
            "operation": "OP-FEATHER-INSERT / 植毛",
            "scope": "适用于所有雷尼克斯及授权 OEM 羽毛球的 16 根毛片定位、植入和首件确认。",
            "ppe": "佩戴指套和护目镜；植毛夹具运动范围内禁止放置散件。",
            "tools": "植毛夹具、16孔定位模板、角度样板、毫米卡尺、首件样球。",
            "parameters": [
                ("单球植毛数量", "16 根", "16-16 根", "逐球计数/模板确认"),
                ("圆周间距", "均匀分布", "相邻夹角约 22.5°", "定位模板复核"),
                ("植入状态", "深度一致", "无松动、劈裂、反羽", "旋转目检"),
            ],
            "steps": [
                "扫描球头批次、毛片批次和生产任务，确认二者均为可用状态。",
                "将球头固定于夹具中心，按定位模板依次植入 16 根已配组毛片。",
                "每植入 4 根检查一次方向、深度和间距，禁止用强扭方式修正毛片。",
                "完成后旋转一周检查圆周均匀性，使用角度样板复核首件。",
                "首件通过后批量作业；每球完成数量确认并绑定在制批次。",
            ],
            "quality": ["每球恰好 16 根毛片", "圆周分布均匀且无反羽", "球头无植孔劈裂和毛片松动"],
            "abnormal": "出现漏植、连续松动、球头劈裂或定位夹具偏移时立即停工，并隔离自上次合格巡检后的产品。",
            "records": common_records,
        },
        {
            "filename": "sop-thread-wind-v1.0.pdf",
            "code": "LNX-SOP-06",
            "title": "绕线作业指导书",
            "operation": "OP-THREAD-WIND / 绕线",
            "scope": "适用于所有产品植毛后毛片根部的绕线固定、张力确认和外观检查。",
            "ppe": "佩戴护目镜和防割指套；调整张力器前必须停止设备。",
            "tools": "绕线设备、校准张力计、剪线器、首件样球、设备点检表。",
            "parameters": [
                ("绕线张力", "4.0 N", "3.5-4.5 N", "首件及换线后测量"),
                ("绕线外观", "紧密平整", "无跳线、断线、松圈", "旋转目检"),
                ("线头处理", "固定可靠", "不得翘起或外露", "手感复核"),
            ],
            "steps": [
                "确认设备点检完成，扫描任务和线材批次，检查线材状态。",
                "穿线后用校准张力计设定 4.0 N，首件实测必须在 3.5-4.5 N。",
                "按工艺方向连续绕线，保持线圈贴合，不得用手接触运行夹具。",
                "剪线并固定线头，旋转检查跳线、松圈和毛片位移。",
                "换线、调机或停机重启后重新做首件，记录实测张力。",
            ],
            "quality": ["张力 3.5-4.5 N", "绕线紧密且无断线、跳线", "毛片位置未因绕线发生偏移"],
            "abnormal": "张力超限或连续出现断线时停止生产，隔离自最近一次合格张力确认后的在制品并复检。",
            "records": common_records + ["张力计编号及校准状态"],
        },
        {
            "filename": "sop-glue-cure-v1.0.pdf",
            "code": "LNX-SOP-07",
            "title": "涂胶固化作业指导书",
            "operation": "OP-GLUE-CURE / 涂胶固化",
            "scope": "适用于绕线完成后的定量涂胶、固化炉装载、温度/时间监控和放行。",
            "ppe": "佩戴耐化学手套和护目镜；保持通风，清理胶液时按化学品作业要求执行。",
            "tools": "定量点胶机、固化炉、计时器、温度记录仪、0.01 g 电子秤。",
            "parameters": [
                ("单球涂胶量", "0.60 ml", "0.50-0.70 ml", "首件称量/设备校核"),
                ("固化温度", "45 °C", "42-48 °C", "连续温度记录"),
                ("固化时间", "45 min", "40-50 min", "批次计时"),
            ],
            "steps": [
                "核对胶液物料批次、有效期和工单，完成点胶机及固化炉开班点检。",
                "设定单球 0.60 ml，首件称量确认后方可批量点胶。",
                "沿绕线区域均匀涂胶，避免堆胶、漏胶和污染毛片。",
                "同批次装盘后记录入炉时间，45 °C 固化 45 min，并保存温度曲线。",
                "出炉后冷却至可操作温度，检查胶层和牢固度，扫描移交速度检测工序。",
            ],
            "quality": ["涂胶量 0.50-0.70 ml", "温度 42-48 °C 且时间 40-50 min", "胶层均匀、无开裂和污染"],
            "abnormal": "温度、时间或点胶量超限时不得自动放行；隔离该炉次并由质量工程师评估复检或返工。",
            "records": common_records + ["固化炉编号、入/出炉时间和温度曲线"],
        },
    ] + flight_sops() + package_sops()


def flight_sops() -> list[dict]:
    specs = [
        ("sop-flight-test-t300-76-v1.0.pdf", "LNX-SOP-08", "T300-76速度与飞行检测指导书", "雷尼克斯 T300 训练级 76 速", "76", "4.90 g", "4.85-4.95 g", "500 mm", "5 只"),
        ("sop-flight-test-t300-77-v1.0.pdf", "LNX-SOP-09", "T300-77速度与飞行检测指导书", "雷尼克斯 T300 训练级 77 速", "77", "5.00 g", "4.95-5.05 g", "500 mm", "5 只"),
        ("sop-flight-test-m700-77-v1.0.pdf", "LNX-SOP-10", "M700-77速度与飞行检测指导书", "雷尼克斯 M700 比赛级 77 速", "77", "5.00 g", "4.95-5.05 g", "300 mm", "8 只"),
        ("sop-flight-test-m700-78-v1.0.pdf", "LNX-SOP-11", "M700-78速度与飞行检测指导书", "雷尼克斯 M700 比赛级 78 速", "78", "5.10 g", "5.05-5.15 g", "300 mm", "8 只"),
        ("sop-flight-test-c500-77-v1.0.pdf", "LNX-SOP-12", "迅羽C500-77速度与飞行检测指导书", "迅羽 C500 OEM 俱乐部级 77 速", "77", "5.00 g", "4.95-5.05 g", "400 mm", "5 只"),
    ]
    rows = []
    for filename, code, title, product, speed, target_weight, weight, dispersion, sample in specs:
        rows.append(
            {
                "filename": filename,
                "code": code,
                "title": title,
                "operation": "OP-SPEED-TEST / 速度与飞行检测",
                "scope": f"适用于 {product} 固化后的过程/最终速度等级、重量和飞行稳定性检验。",
                "ppe": "保持试飞通道净空；检测人员站在警戒线外，拾球前确认发射设备停止。",
                "tools": "校准电子秤、标准环境速度检测装置、定点发射设备、落点标记纸、毫米卷尺。",
                "parameters": [
                    ("速度等级", speed, f"必须等于 {speed}", "标准环境检测"),
                    ("单球重量", target_weight, weight, "0.01 g 电子秤"),
                    ("最大落点离散", f"≤ {dispersion}", f"0-{dispersion}", f"每批抽样 {sample}"),
                ],
                "steps": [
                    "核对产品编码、在制批次、检验方案和设备校准状态，确认环境条件稳定。",
                    f"按检验方案抽取 {sample}，逐只称重并记录原始数据，超出 {weight} 判定不合格。",
                    f"使用标准环境检测装置确认速度等级，结果必须等于 {speed}。",
                    f"按统一方向和力度发射样球，标记落点并计算最大径向离散，限值为 {dispersion}。",
                    "复核外观、球头直径和毛片状态；录入 MES 检验结果后提交质量判定。",
                ],
                "quality": [f"速度等级等于 {speed}", f"重量处于 {weight}", f"落点离散不超过 {dispersion}"],
                "abnormal": "任一关键项目不合格时锁定对应批次，禁止进入包装；按检验方案复检并关联不合格品处置单。",
                "records": ["检验单号和产品批次", "样本编号及逐项实测值", "检测设备编号/校准状态", "检验员、复核人和判定时间"],
            }
        )
    return rows


def package_sops() -> list[dict]:
    specs = [
        ("sop-package-t300-v1.0.pdf", "LNX-SOP-13", "T300系列包装作业指导书", "雷尼克斯 T300 系列", "LNX-T300", "训练级蓝色标识", "雷尼克斯自有品牌标签"),
        ("sop-package-m700-v1.0.pdf", "LNX-SOP-14", "M700系列包装作业指导书", "雷尼克斯 M700 系列", "LNX-M700", "比赛级金色标识", "雷尼克斯自有品牌标签"),
        ("sop-package-c500-oem-v1.0.pdf", "LNX-SOP-15", "迅羽C500-OEM包装作业指导书", "迅羽 C500 OEM 77 速", "XY-C500-OEM", "迅羽 OEM 专用标识", "迅羽客户标签，禁止使用雷尼克斯成品标签"),
    ]
    rows = []
    for filename, code, title, product, label_code, grade_mark, label_rule in specs:
        rows.append(
            {
                "filename": filename,
                "code": code,
                "title": title,
                "operation": "OP-PACK / 成品包装",
                "scope": f"适用于 {product} 终检合格后的装筒、贴标、装箱和成品条码绑定。",
                "ppe": "使用封箱器时佩戴防割手套；包装线通道保持畅通，堆码不得超过现场限高。",
                "tools": "产品专用球筒、外箱、标签打印机、条码扫描枪、封箱器、电子秤。",
                "parameters": [
                    ("每筒数量", "12 只", "12-12 只", "逐筒计数"),
                    ("每外箱", "25 筒", "25-25 筒", "装箱复核"),
                    ("标签规则", label_code, grade_mark, label_rule),
                ],
                "steps": [
                    "扫描产品批次并确认最终检验为合格，未放行批次不得进入包装工位。",
                    f"清线并核对 {label_rule}，现场只保留当前产品对应的球筒、外箱和标签。",
                    "按毛片朝向一致原则每筒装入 12 只，检查筒盖、外观和速度等级标识。",
                    "打印并扫描产品批次码/外箱码，建立 1 箱 25 筒的条码关联。",
                    "复核箱唛、数量、产品型号和客户信息后封箱，移交成品入库并保留追溯记录。",
                ],
                "quality": ["最终检验已放行", "每筒 12 只、每箱 25 筒", "产品、标签、外箱和条码四者一致"],
                "abnormal": "发现混标、混装、条码重复或数量差异时立即停线清点，锁定受影响外箱并重新执行标签隔离确认。",
                "records": ["产品批次和最终检验单", "球筒/外箱物料批次", "产品码与外箱码关联", "包装员、复核人和入库交接时间"],
            }
        )
    return rows


def register_font() -> str:
    if not FONT_PATH.is_file():
        raise FileNotFoundError(f"Chinese font not found: {FONT_PATH}")
    pdfmetrics.registerFont(TTFont("RenixCN", str(FONT_PATH)))
    return "RenixCN"


def styles(font_name: str):
    base = getSampleStyleSheet()
    return {
        "title": ParagraphStyle("TitleCN", parent=base["Title"], fontName=font_name, fontSize=20,
                                leading=26, textColor=NAVY, alignment=TA_CENTER, spaceAfter=8),
        "subtitle": ParagraphStyle("SubtitleCN", parent=base["Normal"], fontName=font_name, fontSize=9.5,
                                   leading=14, textColor=MUTED, alignment=TA_CENTER),
        "heading": ParagraphStyle("HeadingCN", parent=base["Heading2"], fontName=font_name, fontSize=12,
                                  leading=17, textColor=NAVY, spaceBefore=7, spaceAfter=5),
        "body": ParagraphStyle("BodyCN", parent=base["BodyText"], fontName=font_name, fontSize=9.5,
                               leading=15, textColor=INK, alignment=TA_LEFT),
        "small": ParagraphStyle("SmallCN", parent=base["BodyText"], fontName=font_name, fontSize=8.2,
                                leading=12, textColor=INK),
        "white": ParagraphStyle("WhiteCN", parent=base["BodyText"], fontName=font_name, fontSize=8.7,
                                leading=12, textColor=WHITE, alignment=TA_CENTER),
        "alert": ParagraphStyle("AlertCN", parent=base["BodyText"], fontName=font_name, fontSize=9.3,
                                leading=15, textColor=ALERT),
    }


def p(text: object, style) -> Paragraph:
    return Paragraph(str(text).replace("&", "&amp;"), style)


def info_table(record: dict, st: dict) -> Table:
    data = [
        [p("文件编号", st["white"]), p(record["code"], st["small"]),
         p("版本", st["white"]), p(VERSION, st["small"])],
        [p("生效日期", st["white"]), p(EFFECTIVE_DATE, st["small"]),
         p("受控状态", st["white"]), p("RELEASED / 已发布", st["small"])],
        [p("对应工序", st["white"]), p(record["operation"], st["small"]),
         p("数据集", st["white"]), p("RENIX_SYSTEM_BASE_V2", st["small"])],
    ]
    table = Table(data, colWidths=[28 * mm, 66 * mm, 28 * mm, 56 * mm], rowHeights=[9 * mm] * 3)
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (0, -1), NAVY), ("BACKGROUND", (2, 0), (2, -1), NAVY),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"), ("GRID", (0, 0), (-1, -1), 0.5, LINE),
        ("LEFTPADDING", (0, 0), (-1, -1), 6), ("RIGHTPADDING", (0, 0), (-1, -1), 6),
    ]))
    return table


def parameter_table(record: dict, st: dict) -> Table:
    rows = [[p(x, st["white"]) for x in ("控制项目", "目标值", "允许范围/要求", "检查方法")]]
    rows.extend([[p(cell, st["small"]) for cell in row] for row in record["parameters"]])
    table = Table(rows, colWidths=[37 * mm, 37 * mm, 51 * mm, 53 * mm], repeatRows=1)
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), TEAL), ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("GRID", (0, 0), (-1, -1), 0.5, LINE),
        ("BACKGROUND", (0, 1), (-1, -1), colors.white),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, PALE_TEAL]),
        ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]))
    return table


def numbered_rows(items: list[str], st: dict) -> Table:
    data = [[p(str(index), st["white"]), p(item, st["body"])] for index, item in enumerate(items, 1)]
    table = Table(data, colWidths=[12 * mm, 166 * mm])
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (0, -1), NAVY), ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("GRID", (0, 0), (-1, -1), 0.4, LINE),
        ("ROWBACKGROUNDS", (1, 0), (1, -1), [colors.white, PALE_BLUE]),
        ("TOPPADDING", (0, 0), (-1, -1), 6), ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]))
    return table


def bullet_table(items: list[str], st: dict) -> Table:
    data = [[p("-", st["body"]), p(item, st["body"])] for item in items]
    table = Table(data, colWidths=[8 * mm, 170 * mm])
    table.setStyle(TableStyle([
        ("VALIGN", (0, 0), (-1, -1), "TOP"), ("BACKGROUND", (0, 0), (-1, -1), PALE_BLUE),
        ("BOX", (0, 0), (-1, -1), 0.5, LINE),
        ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]))
    return table


def on_page(canvas, doc, record: dict, font_name: str):
    canvas.saveState()
    width, height = A4
    canvas.setFillColor(NAVY)
    canvas.rect(0, height - 13 * mm, width, 13 * mm, stroke=0, fill=1)
    canvas.setFillColor(WHITE)
    canvas.setFont(font_name, 9)
    canvas.drawString(16 * mm, height - 8.2 * mm, COMPANY)
    canvas.drawRightString(width - 16 * mm, height - 8.2 * mm, f"{record['code']}  {VERSION}")
    canvas.setStrokeColor(LINE)
    canvas.line(16 * mm, 13 * mm, width - 16 * mm, 13 * mm)
    canvas.setFillColor(MUTED)
    canvas.setFont(font_name, 8)
    canvas.drawString(16 * mm, 8 * mm, "受控文件 - 仅限 MES 当前发布版本使用")
    canvas.drawRightString(width - 16 * mm, 8 * mm, f"第 {canvas.getPageNumber()} 页")
    canvas.restoreState()


def build_pdf(record: dict, output_path: Path, font_name: str):
    st = styles(font_name)
    doc = SimpleDocTemplate(
        str(output_path), pagesize=A4, rightMargin=16 * mm, leftMargin=16 * mm,
        topMargin=20 * mm, bottomMargin=18 * mm,
        title=record["title"], author=COMPANY, subject="MES作业指导书",
    )
    story = [
        Spacer(1, 4 * mm), p(record["title"], st["title"]),
        p("雷尼克斯 MES 标准作业文件", st["subtitle"]), Spacer(1, 5 * mm),
        info_table(record, st),
        p("1. 目的与适用范围", st["heading"]), p(record["scope"], st["body"]),
        p("2. 安全与作业准备", st["heading"]),
        KeepTogether([p("安全要求", st["small"]), p(record["ppe"], st["alert"])]),
        Spacer(1, 2 * mm), p("设备、工装与量具", st["small"]), p(record["tools"], st["body"]),
        p("3. 关键过程参数", st["heading"]), parameter_table(record, st),
        p("4. 标准作业步骤", st["heading"]), numbered_rows(record["steps"], st),
        PageBreak(),
        Spacer(1, 4 * mm), p("质量控制与记录要求", st["title"]),
        p(f"{record['title']} / {record['code']} / {VERSION}", st["subtitle"]), Spacer(1, 5 * mm),
        p("5. 质量检查点", st["heading"]), bullet_table(record["quality"], st),
        p("6. 异常处置", st["heading"]),
        Table([[p("停止 - 隔离 - 上报 - 复判", st["white"]), p(record["abnormal"], st["alert"])]],
              colWidths=[48 * mm, 130 * mm], style=TableStyle([
                  ("BACKGROUND", (0, 0), (0, 0), ALERT), ("BOX", (0, 0), (-1, -1), 0.6, ALERT),
                  ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                  ("TOPPADDING", (0, 0), (-1, -1), 8), ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
              ])),
        p("7. MES记录要求", st["heading"]), bullet_table(record["records"], st),
        p("8. 放行规则", st["heading"]),
        p("仅当本指导书规定的关键参数、质量检查点、批次追溯和人员记录全部完整，且异常已闭环时，"
          "方可在 MES 中完成本工序并移交下一工序。任何口头放行均不能替代系统记录。", st["body"]),
        p("9. 文件批准", st["heading"]),
        Table([
            [p("编制", st["white"]), p("生产技术", st["small"]), p("审核", st["white"]), p("质量管理", st["small"]), p("批准", st["white"]), p("生产负责人", st["small"])],
            [p("生效日期", st["white"]), p(EFFECTIVE_DATE, st["small"]), p("版本", st["white"]), p(VERSION, st["small"]), p("状态", st["white"]), p("已发布", st["small"])],
        ], colWidths=[22 * mm, 37 * mm] * 3, rowHeights=[11 * mm, 11 * mm], style=TableStyle([
            ("BACKGROUND", (0, 0), (0, -1), NAVY), ("BACKGROUND", (2, 0), (2, -1), NAVY),
            ("BACKGROUND", (4, 0), (4, -1), NAVY), ("GRID", (0, 0), (-1, -1), 0.5, LINE),
            ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ])),
        Spacer(1, 5 * mm),
        p("版本说明：V1.0 为企业基础数据最终版首次发布。纸质打印件为非受控副本，使用前须核对 MES 当前版本。", st["small"]),
    ]
    doc.build(story, onFirstPage=lambda c, d: on_page(c, d, record, font_name),
              onLaterPages=lambda c, d: on_page(c, d, record, font_name))


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path,
                        default=Path("相关文档，图片/sop/雷尼克斯MES作业指导书"))
    parser.add_argument("--deploy-root", type=Path, default=Path(r"D:\upload\file"))
    parser.add_argument("--no-deploy", action="store_true")
    return parser.parse_args()


def main():
    args = parse_args()
    font_name = register_font()
    args.output.mkdir(parents=True, exist_ok=True)
    records = sop_records()
    if len(records) != 15 or len({record["filename"] for record in records}) != 15:
        raise RuntimeError("Expected exactly 15 unique work instructions")
    for record in records:
        build_pdf(record, args.output / record["filename"], font_name)
    if not args.no_deploy:
        deploy_dir = args.deploy_root / "sop"
        deploy_dir.mkdir(parents=True, exist_ok=True)
        for record in records:
            shutil.copy2(args.output / record["filename"], deploy_dir / record["filename"])
    print(f"generated={len(records)} output={args.output.resolve()}")
    if not args.no_deploy:
        print(f"deployed={len(records)} output={(args.deploy_root / 'sop').resolve()}")


if __name__ == "__main__":
    main()

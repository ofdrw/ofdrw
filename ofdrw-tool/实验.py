import os
from ofdrw import OFDDocument


def convert_ofd_to_pdf(ofd_path, pdf_path):
    # 打开OFD文件
    doc = OFDDocument(ofd_path)

    # 导出为PDF
    doc.export(pdf_path)


# 定义文件路径
ofd_file = r'c:\7.ofd'
pdf_file = r'c:\7.pdf'

# 转换
convert_ofd_to_pdf(ofd_file, pdf_file)


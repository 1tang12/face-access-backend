import numpy as np
import torch

from nets.facenet import Facenet as facenet
from utils.utils import preprocess_input, resize_image, show_config


class Facenet(object):
    _defaults = {
        "model_path": "model_data/facenet_mobilenet.pth",
        "input_shape": [160, 160, 3],
        "backbone": "mobilenet",
        "letterbox_image": True,
        "cuda": False,
    }

    @classmethod
    def get_defaults(cls, n):
        if n in cls._defaults:
            return cls._defaults[n]
        return "Unrecognized attribute name '" + n + "'"

    def __init__(self, **kwargs):
        self.__dict__.update(self._defaults)
        for name, value in kwargs.items():
            setattr(self, name, value)

        self.generate()
        show_config(**self._defaults)

    def generate(self):
        print("Loading weights into state dict...")

        if torch.backends.mps.is_available():
            self.device = torch.device("mps")
        else:
            self.device = torch.device("cpu")

        self.net = facenet(backbone=self.backbone, mode="predict").eval()
        try:
            state_dict = torch.load(self.model_path, map_location=self.device, weights_only=True)
        except TypeError:
            state_dict = torch.load(self.model_path, map_location=self.device)

        self.net.load_state_dict(state_dict, strict=False)
        self.net = self.net.to(self.device)

        print(f"{self.model_path} model loaded.")
        print(f"Using device: {self.device}")

    def _image_to_tensor(self, image):
        image = resize_image(
            image,
            [self.input_shape[1], self.input_shape[0]],
            letterbox_image=self.letterbox_image
        )

        photo = torch.from_numpy(
            np.expand_dims(
                np.transpose(
                    preprocess_input(np.array(image, dtype=np.float32)),
                    (2, 0, 1)
                ),
                0
            )
        ).float()

        return image, photo.to(self.device)

    def get_feature(self, image):
        with torch.no_grad():
            _, photo = self._image_to_tensor(image)
            output = self.net(photo).cpu().numpy()[0]
        return output

    def compare_feature(self, feature1, feature2):
        return float(np.linalg.norm(feature1 - feature2))

    def detect_image(self, image_1, image_2):
        import matplotlib.pyplot as plt

        with torch.no_grad():
            image_1, photo_1 = self._image_to_tensor(image_1)
            image_2, photo_2 = self._image_to_tensor(image_2)

            output1 = self.net(photo_1).cpu().numpy()
            output2 = self.net(photo_2).cpu().numpy()

            l1 = np.linalg.norm(output1 - output2, axis=1)
            distance = float(l1[0])

        plt.subplot(1, 2, 1)
        plt.imshow(np.array(image_1))
        plt.title("Image 1")
        plt.axis("off")

        plt.subplot(1, 2, 2)
        plt.imshow(np.array(image_2))
        plt.title("Image 2")
        plt.axis("off")

        plt.figtext(0.5, 0.02, f"Distance: {distance:.3f}", ha="center", fontsize=12)
        plt.show()

        return distance

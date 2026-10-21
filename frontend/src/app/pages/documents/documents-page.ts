import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { DocumentDetail, DocumentSummary } from '../../core/api.models';
import { ApiService, errorMessage } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';

const ACCEPTED_TYPES = ['application/pdf', 'image/png', 'image/jpeg', 'image/tiff'];

@Component({
  selector: 'app-documents-page',
  imports: [DatePipe, DecimalPipe],
  templateUrl: './documents-page.html',
  styleUrl: './documents-page.scss'
})
export class DocumentsPage implements OnInit {
  private readonly api = inject(ApiService);
  /** Seul un ADMIN peut importer ou supprimer (l'API le vérifie aussi : ceci n'est que l'affichage). */
  protected readonly auth = inject(AuthService);

  protected readonly documents = signal<DocumentSummary[]>([]);
  protected readonly loading = signal(true);
  protected readonly uploadingFile = signal<string | null>(null);
  protected readonly dragOver = signal(false);
  protected readonly message = signal<{ type: 'success' | 'error'; text: string } | null>(null);
  protected readonly selected = signal<DocumentDetail | null>(null);

  protected readonly accept = ACCEPTED_TYPES.join(',');

  ngOnInit(): void {
    this.refresh();
  }

  protected async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      this.documents.set(await firstValueFrom(this.api.listDocuments()));
    } catch (e) {
      this.message.set({ type: 'error', text: errorMessage(e) });
    } finally {
      this.loading.set(false);
    }
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(true);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    this.upload(Array.from(event.dataTransfer?.files ?? []));
  }

  protected onFileInput(input: HTMLInputElement): void {
    this.upload(Array.from(input.files ?? []));
    input.value = '';
  }

  /** Importe les fichiers un par un (l'OCR et l'indexation prennent quelques secondes chacun). */
  private async upload(files: File[]): Promise<void> {
    if (this.uploadingFile() || files.length === 0) {
      return;
    }
    this.message.set(null);
    const imported: string[] = [];
    for (const file of files) {
      if (!ACCEPTED_TYPES.includes(file.type)) {
        this.message.set({ type: 'error', text: `« ${file.name} » : format non supporté (PDF, PNG, JPEG ou TIFF).` });
        continue;
      }
      this.uploadingFile.set(file.name);
      try {
        const doc = await firstValueFrom(this.api.uploadDocument(file));
        imported.push(`« ${doc.fileName} »${doc.extractionMethod === 'OCR' ? ' (OCR)' : ''}`);
      } catch (e) {
        this.message.set({ type: 'error', text: `« ${file.name} » : ${errorMessage(e)}` });
      }
    }
    this.uploadingFile.set(null);
    if (imported.length > 0 && this.message()?.type !== 'error') {
      this.message.set({ type: 'success', text: `Importé et indexé : ${imported.join(', ')}` });
    }
    await this.refresh();
  }

  protected async show(doc: DocumentSummary): Promise<void> {
    if (this.selected()?.id === doc.id) {
      this.selected.set(null);
      return;
    }
    try {
      this.selected.set(await firstValueFrom(this.api.getDocument(doc.id)));
    } catch (e) {
      this.message.set({ type: 'error', text: errorMessage(e) });
    }
  }

  protected async remove(doc: DocumentSummary): Promise<void> {
    if (!confirm(`Supprimer « ${doc.fileName} » ? Ses passages ne seront plus utilisés par l'assistant.`)) {
      return;
    }
    try {
      await firstValueFrom(this.api.deleteDocument(doc.id));
      if (this.selected()?.id === doc.id) {
        this.selected.set(null);
      }
      this.message.set({ type: 'success', text: `« ${doc.fileName} » a été supprimé.` });
      await this.refresh();
    } catch (e) {
      this.message.set({ type: 'error', text: errorMessage(e) });
    }
  }

  protected formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} Ko`;
    return `${(bytes / 1024 / 1024).toFixed(1)} Mo`;
  }
}
